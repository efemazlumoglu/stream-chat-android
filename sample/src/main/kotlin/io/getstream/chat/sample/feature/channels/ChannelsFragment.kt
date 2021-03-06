package io.getstream.chat.sample.feature.channels

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.getstream.sdk.chat.Chat
import com.getstream.sdk.chat.viewmodel.channels.ChannelsViewModel
import com.getstream.sdk.chat.viewmodel.channels.ChannelsViewModelImpl
import com.getstream.sdk.chat.viewmodel.channels.bindView
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.socket.InitConnectionListener
import io.getstream.chat.sample.R
import io.getstream.chat.sample.common.image
import io.getstream.chat.sample.common.name
import io.getstream.chat.sample.data.user.UserRepository
import kotlinx.android.synthetic.main.fragment_channels.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class ChannelsFragment : Fragment(R.layout.fragment_channels) {
    private val userRepo: UserRepository by inject()
    private val viewModel by lazy { ChannelsViewModelImpl() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val loggedInUser = userRepo.user
        val user = User().apply {
            id = loggedInUser.id
            image = loggedInUser.image
            name = loggedInUser.name
        }

        Chat.getInstance().setUser(
            user, loggedInUser.token,
            object : InitConnectionListener() {
                override fun onSuccess(data: ConnectionData) {
                    Timber.d("User set successfully")
                }

                override fun onError(error: ChatError) {
                    Timber.e("Failed to set user")
                }
            }
        )

        viewModel.bindView(channelsListView, this@ChannelsFragment)

        viewModel.state.observe(
            viewLifecycleOwner,
            Observer {
                if (ChannelsViewModel.State.NavigateToLoginScreen == it) {
                    findNavController().navigate(R.id.action_to_loginFragment)
                }
            }
        )

        setupOnClickListeners()
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.menu_channels)
    }

    private fun setupOnClickListeners() {
        channelsListView.setOnChannelClickListener {
            findNavController().navigate(ChannelsFragmentDirections.actionOpenChannel(it.cid))
        }

        addNewChannelButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_create_channel)
        }

        activity?.apply {
            onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        activity?.finish()
                    }
                }
            )
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_log_out -> {
                    viewModel.onEvent(ChannelsViewModel.Event.LogoutClicked)
                    true
                }
                else -> false
            }
        }
    }
}
