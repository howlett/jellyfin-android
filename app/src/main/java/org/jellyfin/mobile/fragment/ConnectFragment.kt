package org.jellyfin.mobile.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.api.client.KtorClient
import org.jellyfin.apiclient.api.operations.SystemApi
import org.jellyfin.apiclient.discovery.LocalServerDiscovery
import org.jellyfin.apiclient.model.api.PublicSystemInfo
import org.jellyfin.apiclient.model.discovery.DiscoveryServerInfo
import org.jellyfin.mobile.R
import org.jellyfin.mobile.controller.ServerController
import org.jellyfin.mobile.databinding.FragmentConnectBinding
import org.jellyfin.mobile.utils.Constants
import org.jellyfin.mobile.utils.applyWindowInsetsAsMargins
import org.jellyfin.mobile.viewmodel.MainViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class ConnectFragment : Fragment() {
    private val mainViewModel: MainViewModel by sharedViewModel()
    private val jellyfin: Jellyfin by inject()
    private val apiClient by inject<KtorClient>()
    private val systemApi by inject<SystemApi>()
    private val serverController: ServerController by inject()

    // UI
    private var _connectServerBinding: FragmentConnectBinding? = null
    private val connectServerBinding get() = _connectServerBinding!!
    private val serverSetupLayout: View get() = connectServerBinding.root
    private val hostInput: EditText get() = connectServerBinding.hostInput
    private val connectionErrorText: TextView get() = connectServerBinding.connectionErrorText
    private val connectButton: Button get() = connectServerBinding.connectButton
    private val chooseServerButton: Button get() = connectServerBinding.chooseServerButton

    private val serverList = ArrayList<DiscoveryServerInfo>(LocalServerDiscovery.DISCOVERY_MAX_SERVERS)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _connectServerBinding = FragmentConnectBinding.inflate(inflater, container, false)
        return serverSetupLayout.apply { applyWindowInsetsAsMargins() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets
        ViewCompat.requestApplyInsets(serverSetupLayout)

        hostInput.setText(mainViewModel.serverState.value.server?.hostname)
        hostInput.setSelection(hostInput.length())
        hostInput.setOnEditorActionListener { _, action, event ->
            when {
                action == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                    connect()
                    true
                }
                else -> false
            }
        }
        connectButton.setOnClickListener {
            connect()
        }
        chooseServerButton.setOnClickListener {
            chooseServer()
        }

        if (arguments?.getBoolean(Constants.FRAGMENT_CONNECT_EXTRA_ERROR) == true)
            showConnectionError()

        // Show keyboard
        serverSetupLayout.doOnNextLayout {
            hostInput.postDelayed(25) {
                hostInput.requestFocus()

                requireContext().getSystemService<InputMethodManager>()?.showSoftInput(hostInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        discoverServers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _connectServerBinding = null
    }

    private fun connect(enteredUrl: String = hostInput.text.toString()) {
        hostInput.isEnabled = false
        connectButton.isEnabled = false
        clearConnectionError()

        lifecycleScope.launch {
            val httpUrl = checkServerUrlAndConnection(enteredUrl)
            if (httpUrl != null) {
                clearServerList()
                serverController.setupServer(httpUrl.toString())
                mainViewModel.refreshServer()
            }
            hostInput.isEnabled = true
            connectButton.isEnabled = true
        }
    }

    private fun discoverServers() {
        lifecycleScope.launch {
            jellyfin.discovery.discoverLocalServers(maxServers = LocalServerDiscovery.DISCOVERY_MAX_SERVERS).flowOn(Dispatchers.IO).collect { serverInfo ->
                serverList.add(serverInfo)
                chooseServerButton.isVisible = true
            }
        }
    }

    private fun chooseServer() {
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.available_servers_title)
            setItems(serverList.map { "${it.name}\n${it.address}" }.toTypedArray()) { _, index ->
                connect(serverList[index].address)
            }
        }.show()
    }

    private fun clearServerList() {
        serverList.clear()
        chooseServerButton.isVisible = false
    }

    private fun showConnectionError(@StringRes errorString: Int = R.string.connection_error_cannot_connect) {
        connectionErrorText.setText(errorString)
        connectionErrorText.isVisible = true
    }

    private fun clearConnectionError() {
        connectionErrorText.isVisible = false
    }

    // TODO use discovery functions to discover correct URL
    private suspend fun checkServerUrlAndConnection(enteredUrl: String): HttpUrl? {
        Timber.i("checkServerUrlAndConnection $enteredUrl")

        val urls = jellyfin.discovery.getAddressCandidates(enteredUrl)
        Timber.i("Address candidates are $urls")

        var httpUrl: HttpUrl? = null
        var serverInfo: PublicSystemInfo? = null
        loop@ for (url in urls) {
            httpUrl = url.toHttpUrlOrNull()

            if (httpUrl == null) {
                showConnectionError(R.string.connection_error_invalid_format)
                return null // Format is invalid, don't try any other variants
            }

            // Set API client address
            apiClient.baseUrl = httpUrl.toString()

            try {
                val response = systemApi.getPublicSystemInfo()
                if (response.status == 200) {
                    serverInfo = response.content
                    break@loop
                }
            } catch (err: Error) {
                // Connection issues ocurred
            }
        }

        if (httpUrl == null || serverInfo == null) {
            Timber.w("Failed to find server info, url was $httpUrl")

            showConnectionError()
            return null
        }

        val version = serverInfo.version
            ?.split('.')
            ?.mapNotNull(String::toIntOrNull)
            ?: emptyList()

        val isValidInstance = when {
            version.size != 3 -> false
            // Major version is invalid
            version[0] != Jellyfin.recommendedVersion.major -> false
            // Minor version is too old
            version[1] < Jellyfin.recommendedVersion.minor -> false
            // Incorrect product name
            serverInfo.productName != "Jellyfin Server" -> false
            else -> true
        }

        Timber.i("Server at $httpUrl with version ${serverInfo.version} valid: $isValidInstance")

        if (!isValidInstance) {
            showConnectionError(R.string.connection_error_invalid_version)
            return null
        }

        return httpUrl
    }
}
