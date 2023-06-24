package com.mifos.mifosxdroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.test.espresso.IdlingResource
import com.google.android.material.navigation.NavigationView
import com.mifos.mifosxdroid.activity.pathtracking.PathTrackingActivity
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.databinding.ActivityHomeBinding
import com.mifos.mifosxdroid.databinding.ViewNavDrawerHeaderBinding
import com.mifos.mifosxdroid.offline.offlinedashbarod.OfflineDashboardFragment
import com.mifos.mifosxdroid.online.GenerateCollectionSheetActivity
import com.mifos.mifosxdroid.online.RunReportsActivity
import com.mifos.mifosxdroid.online.centerlist.CenterListFragment
import com.mifos.mifosxdroid.online.checkerinbox.CheckerInboxPendingTasksActivity
import com.mifos.mifosxdroid.online.clientlist.ClientListFragment
import com.mifos.mifosxdroid.online.groupslist.GroupsListFragment
import com.mifos.mifosxdroid.online.search.SearchFragment
import com.mifos.utils.Constants
import com.mifos.utils.EspressoIdlingResource
import com.mifos.utils.PrefManager

/**
 * Created by shashankpriyadarshi on 19/06/20.
 */
open class HomeActivity : MifosBaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navHeaderBinding: ViewNavDrawerHeaderBinding

    private lateinit var menu: Menu
    private var onSearchFragment = true
    private lateinit var navController: NavController
    private lateinit var userStatusToggle: SwitchCompat
    private var doubleBackToExitPressedOnce = false
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_home)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration.Builder()
            .setDrawerLayout(binding.drawer)
            .build()
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.navigationView, navController)
        if (savedInstanceState == null) {
            val fragment: Fragment = SearchFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container_a, fragment).commit()
            supportActionBar?.setTitle(R.string.dashboard)
        }
        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    onSearchFragment = true
                    openFragment(SearchFragment())
                    supportActionBar?.setTitle(R.string.dashboard)
                }

                R.id.navigation_client_list -> {
                    onSearchFragment = false
                    openFragment(ClientListFragment())
                    supportActionBar?.setTitle(R.string.clients)
                }

                R.id.navigation_center_list -> {
                    onSearchFragment = false
                    openFragment(CenterListFragment())
                    supportActionBar?.setTitle(R.string.title_activity_centers)
                }

                R.id.navigation_group_list -> {
                    onSearchFragment = false
                    openFragment(GroupsListFragment())
                    supportActionBar?.setTitle(R.string.title_center_list)
                }
            }
            true
        }
        setupNavigationBar()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // ignore the current selected item
        /*if (item.isChecked()) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            return false;
        }*/
        clearFragmentBackStack()
        val intent = Intent()
        when (item.itemId) {
            R.id.item_checker_inbox -> {
                intent.setClass(this, CheckerInboxPendingTasksActivity::class.java)
                startActivity(intent)
            }

            R.id.item_path_tracker -> {
                intent.setClass(applicationContext, PathTrackingActivity::class.java)
                startNavigationClickActivity(intent)
            }

            R.id.item_offline -> {
                replaceFragment(OfflineDashboardFragment.newInstance(), false, R.id.container_a)
                supportActionBar?.setTitle(R.string.offline)
            }

            R.id.individual_collection_sheet -> {
                intent.setClass(this, GenerateCollectionSheetActivity::class.java)
                intent.putExtra(Constants.COLLECTION_TYPE, Constants.EXTRA_COLLECTION_INDIVIDUAL)
                startActivity(intent)
            }

            R.id.collection_sheet -> {
                intent.setClass(this, GenerateCollectionSheetActivity::class.java)
                intent.putExtra(Constants.COLLECTION_TYPE, Constants.EXTRA_COLLECTION_COLLECTION)
                startActivity(intent)
            }

            R.id.item_settings -> {
                intent.setClass(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            R.id.runreport -> {
                intent.setClass(this, RunReportsActivity::class.java)
                startActivity(intent)
            }

            R.id.about -> {
                intent.setClass(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }
        binding.drawer.closeDrawer(GravityCompat.START)
        return false
    }

    /**
     * This SwitchCompat Toggle Handling the User Status.
     * Setting the User Status to Offline or Online
     */
    private fun setupUserStatusToggle(headerView: View) {
        navHeaderBinding = ViewNavDrawerHeaderBinding.bind(headerView)
        userStatusToggle = navHeaderBinding.userStatusToggle

        if (PrefManager.userStatus == Constants.USER_OFFLINE) {
            userStatusToggle.isChecked = true
        }
        userStatusToggle.setOnClickListener(View.OnClickListener {
            if (PrefManager.userStatus == Constants.USER_OFFLINE) {
                PrefManager.userStatus = Constants.USER_ONLINE
                userStatusToggle.isChecked = false
            } else {
                PrefManager.userStatus = Constants.USER_OFFLINE
                userStatusToggle.isChecked = true
            }
        })
    }

    private fun startNavigationClickActivity(intent: Intent?) {
        val handler = Handler()
        handler.postDelayed({ startActivity(intent) }, 500)
    }

    /**
     * downloads the logged in user's username
     * sets dummy profile picture as no profile picture attribute available
     */
    private fun loadClientDetails() {
        // download logged in user
        val loggedInUser = PrefManager.user
        navHeaderBinding.tvUserName.text = loggedInUser.username
        navHeaderBinding.ivUserPicture.setImageResource(R.drawable.ic_dp_placeholder)
    }

    override fun onBackPressed() {
        // check if the nav mDrawer is open
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START)
        } else {
            if (!onSearchFragment) {
                goHomeFragment()
            } else {
                doubleBackToExit()
            }
            onSearchFragment = true
        }
    }

    @get:VisibleForTesting
    val countingIdlingResource: IdlingResource
        get() = EspressoIdlingResource.getIdlingResource()

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    private fun setupNavigationBar() {
//        mNavigationHeader = binding.navigationView.getHeaderView(0)
        binding.navigationView.getHeaderView(0)?.let { setupUserStatusToggle(it) }


        binding.navigationView.setNavigationItemSelectedListener(this as NavigationView.OnNavigationItemSelectedListener)

        // setup drawer layout and sync to toolbar
        val actionBarDrawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            binding.drawer, toolbar, R.string.open_drawer, R.string.close_drawer
        ) {

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                setUserStatus(userStatusToggle)
                hideKeyboard(binding.drawer)
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset != 0f) super.onDrawerSlide(drawerView, slideOffset)
            }
        }
        binding.drawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // make an API call to fetch logged in client's details
        loadClientDetails()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            logout()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openFragment(fragment: Fragment?) {
        val transaction = supportFragmentManager.beginTransaction()
        if (fragment != null) {
            transaction.replace(R.id.container_a, fragment)
        }
        transaction.commit()
    }

    private fun doubleBackToExit() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true

        Toast.makeText(
            this,
            R.string.back_again,
            Toast.LENGTH_SHORT
        ).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun goHomeFragment() {
        val fragmentSearch = SearchFragment()
        binding.navView.selectedItemId = R.id.navigation_dashboard
        supportFragmentManager.beginTransaction().apply {
            add(R.id.container_a, fragmentSearch)
            commit()
        }
    }
}
