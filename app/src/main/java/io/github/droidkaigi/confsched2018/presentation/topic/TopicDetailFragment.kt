package io.github.droidkaigi.confsched2018.presentation.topic

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.github.droidkaigi.confsched2018.databinding.FragmentTopicDetailBinding
import io.github.droidkaigi.confsched2018.di.Injectable
import io.github.droidkaigi.confsched2018.model.Session
import io.github.droidkaigi.confsched2018.presentation.NavigationController
import io.github.droidkaigi.confsched2018.presentation.Result
import io.github.droidkaigi.confsched2018.presentation.common.binding.FragmentDataBindingComponent
import io.github.droidkaigi.confsched2018.presentation.sessions.item.SpeechSessionItem
import io.github.droidkaigi.confsched2018.presentation.sessions.item.SimpleSessionsSection
import io.github.droidkaigi.confsched2018.util.ext.observe
import timber.log.Timber
import javax.inject.Inject


class TopicDetailFragment : Fragment(), Injectable {

    @Inject lateinit var navigationController: NavigationController
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentTopicDetailBinding
    private val sessionsSection = SimpleSessionsSection(this)

    private val topicDetailViewModel: TopicDetailViewModel by lazy {
        ViewModelProviders.of(activity!!, viewModelFactory).get(TopicDetailViewModel::class.java)
    }

    private val onFavoriteClickListener = { session: Session.SpeechSession ->
        session.isFavorited = !session.isFavorited
        binding.sessionsRecycler.adapter.notifyDataSetChanged()

        topicDetailViewModel.onFavoriteClick(session)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentTopicDetailBinding.inflate(
                inflater,
                container!!,
                false,
                FragmentDataBindingComponent(this)
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        topicDetailViewModel.topicId = arguments!!.getInt(EXTRA_TOPIC_ID)
        topicDetailViewModel.topicSessions.observe(this, { result ->
            when (result) {
                is Result.Success -> {
                    sessionsSection.updateSessions(result.data.second, onFavoriteClickListener)
                }
                is Result.Failure -> {
                    Timber.e(result.e)
                }
            }
        })
        lifecycle.addObserver(topicDetailViewModel)
    }

    private fun setupRecyclerView() {
        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(sessionsSection)
            setOnItemClickListener { item, _ ->
                val sessionItem = item as? SpeechSessionItem ?: return@setOnItemClickListener
                navigationController.navigateToSessionDetailActivity(sessionItem.session)
            }
        }
        binding.sessionsRecycler.apply {
            adapter = groupAdapter
        }
    }

    companion object {
        const val EXTRA_TOPIC_ID = "EXTRA_TOPIC_ID"
        fun newInstance(topicId: Int): TopicDetailFragment = TopicDetailFragment().apply {
            arguments = Bundle().apply {
                putInt(EXTRA_TOPIC_ID, topicId)
            }
        }
    }
}
