package io.github.droidkaigi.confsched2018.presentation.detail

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.github.droidkaigi.confsched2018.R
import io.github.droidkaigi.confsched2018.databinding.ActivitySessionDetailBinding
import io.github.droidkaigi.confsched2018.model.Session
import io.github.droidkaigi.confsched2018.presentation.NavigationController
import io.github.droidkaigi.confsched2018.presentation.Result
import io.github.droidkaigi.confsched2018.presentation.common.activity.BaseActivity
import io.github.droidkaigi.confsched2018.presentation.common.menu.DrawerMenu
import io.github.droidkaigi.confsched2018.util.ext.observe
import timber.log.Timber
import javax.inject.Inject

class SessionDetailActivity : BaseActivity(), HasSupportFragmentInjector {
    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var navigationController: NavigationController
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var drawerMenu: DrawerMenu

    private val binding: ActivitySessionDetailBinding by lazy {
        DataBindingUtil
                .setContentView<ActivitySessionDetailBinding>(
                        this,
                        R.layout.activity_session_detail
                )
    }

    private val sessionDetailViewModel: SessionDetailViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SessionDetailViewModel::class.java)
    }

    private val pagerAdapter = SessionDetailFragmentPagerAdapter(supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
        sessionDetailViewModel.sessions.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    val sessions = result.data
                    bindSessions(sessions)
                }
                is Result.Failure -> {
                    Timber.e(result.e)
                }
            }
        }

        binding.detailSessionsPager.adapter = pagerAdapter
        binding.detailSessionsPager.addOnPageChangeListener(
                object : ViewPager.SimpleOnPageChangeListener() {

                    override fun onPageSelected(position: Int) {
                        updateSessionIndicator(position)
                    }
                }
        )
        binding.detailSessionsPrevSession.setOnClickListener {
            binding.detailSessionsPager.currentItem = binding.detailSessionsPager.currentItem - 1
        }
        binding.detailSessionsNextSession.setOnClickListener {
            binding.detailSessionsPager.currentItem = binding.detailSessionsPager.currentItem + 1
        }
        drawerMenu.setup(binding.toolbar, binding.drawerLayout, binding.drawer)
    }

    private fun bindSessions(sessions: List<Session.SpeechSession>) {
        val firstAssign = pagerAdapter.sessions.isEmpty() && sessions.isNotEmpty()
        pagerAdapter.sessions = sessions
        if (firstAssign) {
            val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
            val position = sessions.indexOfFirst { it.id == sessionId }
            binding
                    .detailSessionsPager
                    .setCurrentItem(
                            position,
                            false
                    )
            updateSessionIndicator(position)
        }
    }

    private fun updateSessionIndicator(position: Int) {
        binding.prevSession = pagerAdapter.sessions.getOrNull(position - 1)
        binding.nextSession = pagerAdapter.sessions.getOrNull(position + 1)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = dispatchingAndroidInjector

    class SessionDetailFragmentPagerAdapter(
            fragmentManager: FragmentManager
    ) : FragmentStatePagerAdapter(fragmentManager) {
        var sessions: List<Session.SpeechSession> = listOf()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItem(position: Int): Fragment {
            return SessionDetailFragment.newInstance(sessions[position].id)
        }

        override fun getCount(): Int = sessions.size
    }

    companion object {
        val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        fun start(context: Context, session: Session) {
            context.startActivity(Intent(context, SessionDetailActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, session.id)
            })
        }
    }
}
