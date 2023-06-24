/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */
package com.mifos.mifosxdroid.online.search

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mifos.mifosxdroid.R
import com.mifos.mifosxdroid.adapters.SearchAdapter
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.core.MifosBaseFragment
import com.mifos.mifosxdroid.core.util.Toaster.show
import com.mifos.mifosxdroid.databinding.FragmentClientSearchBinding
import com.mifos.mifosxdroid.online.CentersActivity
import com.mifos.mifosxdroid.online.ClientActivity
import com.mifos.mifosxdroid.online.GroupsActivity
import com.mifos.mifosxdroid.online.createnewcenter.CreateNewCenterFragment
import com.mifos.mifosxdroid.online.createnewclient.CreateNewClientFragment
import com.mifos.mifosxdroid.online.createnewgroup.CreateNewGroupFragment
import com.mifos.objects.SearchedEntity
import com.mifos.utils.Constants
import com.mifos.utils.EspressoIdlingResource
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import javax.inject.Inject

class SearchFragment : MifosBaseFragment(), SearchMvpView, OnItemSelectedListener {

    private lateinit var binding: FragmentClientSearchBinding

    private lateinit var searchOptionsValues: Array<String>
    private lateinit var searchAdapter: SearchAdapter

    @Inject
    lateinit var searchPresenter: SearchPresenter


    // determines weather search is triggered by user or system
    private var autoTriggerSearch = false
    private lateinit var searchedEntities: MutableList<SearchedEntity>
    private lateinit var searchOptionsAdapter: ArrayAdapter<CharSequence>
    private lateinit var resources: String
    private var isFabOpen = false
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var rotateForward: Animation
    private lateinit var rotateBackward: Animation
    private lateinit var layoutManager: LinearLayoutManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as MifosBaseActivity?)?.activityComponent?.inject(this)
        searchedEntities = ArrayList()
        fabOpen = AnimationUtils.loadAnimation(context, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(context, R.anim.fab_close)
        rotateForward = AnimationUtils.loadAnimation(context, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(context, R.anim.rotate_backward)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClientSearchBinding.inflate(inflater, container, false)
        setToolbarTitle(getResources().getString(R.string.dashboard))
        searchPresenter.attachView(this)
        searchOptionsValues =
            requireActivity().resources.getStringArray(R.array.search_options_values)
        showUserInterface()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabClient.setOnClickListener {
            (activity as MifosBaseActivity?)?.replaceFragment(
                CreateNewClientFragment.newInstance(),
                true, R.id.container_a
            )
        }

        binding.fabCenter.setOnClickListener {
            (activity as MifosBaseActivity?)?.replaceFragment(
                CreateNewCenterFragment.newInstance(),
                true, R.id.container_a
            )
        }

        binding.fabGroup.setOnClickListener {
            (activity as MifosBaseActivity?)?.replaceFragment(
                CreateNewGroupFragment.newInstance(),
                true, R.id.container_a
            )
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onClickSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }


        binding.btnSearch.setOnClickListener {
            onClickSearch()
        }

        binding.fabCreate.setOnClickListener {
            if (isFabOpen) {
                binding.fabCreate.startAnimation(rotateBackward)
                binding.fabClient.startAnimation(fabClose)
                binding.fabCenter.startAnimation(fabClose)
                binding.fabGroup.startAnimation(fabClose)
                binding.fabClient.isClickable = false
                binding.fabCenter.isClickable = false
                binding.fabGroup.isClickable = false
                isFabOpen = false
            } else {
                binding.fabCreate.startAnimation(rotateForward)
                binding.fabClient.startAnimation(fabOpen)
                binding.fabCenter.startAnimation(fabOpen)
                binding.fabGroup.startAnimation(fabOpen)
                binding.fabClient.isClickable = true
                binding.fabCenter.isClickable = true
                binding.fabGroup.isClickable = true
                isFabOpen = true
            }
            autoTriggerSearch = false
        }
    }

    override fun showUserInterface() {
        searchOptionsAdapter = ArrayAdapter.createFromResource(
            (requireActivity()),
            R.array.search_options, android.R.layout.simple_spinner_item
        )
        searchOptionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSearch.adapter = searchOptionsAdapter
        binding.spSearch.onItemSelectedListener = this
        binding.etSearch.requestFocus()
        layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvSearch.layoutManager = layoutManager
        binding.rvSearch.setHasFixedSize(true)
        searchAdapter = SearchAdapter { searchedEntity: SearchedEntity ->
            var activity: Intent? = null
            when (searchedEntity.entityType) {
                Constants.SEARCH_ENTITY_LOAN -> {
                    activity = Intent(getActivity(), ClientActivity::class.java)
                    activity.putExtra(
                        Constants.LOAN_ACCOUNT_NUMBER,
                        searchedEntity.entityId
                    )
                }

                Constants.SEARCH_ENTITY_CLIENT -> {
                    activity = Intent(getActivity(), ClientActivity::class.java)
                    activity.putExtra(
                        Constants.CLIENT_ID,
                        searchedEntity.entityId
                    )
                }

                Constants.SEARCH_ENTITY_GROUP -> {
                    activity = Intent(getActivity(), GroupsActivity::class.java)
                    activity.putExtra(
                        Constants.GROUP_ID,
                        searchedEntity.entityId
                    )
                }

                Constants.SEARCH_ENTITY_SAVING -> {
                    activity = Intent(getActivity(), ClientActivity::class.java)
                    activity.putExtra(
                        Constants.SAVINGS_ACCOUNT_NUMBER,
                        searchedEntity.entityId
                    )
                }

                Constants.SEARCH_ENTITY_CENTER -> {
                    activity = Intent(getActivity(), CentersActivity::class.java)
                    activity.putExtra(
                        Constants.CENTER_ID,
                        searchedEntity.entityId
                    )
                }
            }
            startActivity(activity)
        }
        binding.rvSearch.adapter = searchAdapter
        binding.cbExactMatch.setOnCheckedChangeListener { _, _ -> onClickSearch() }
        showGuide()
    }

    private fun showGuide() {
        val config = ShowcaseConfig()
        config.delay = 250 // half second between each showcase view
        val sequence = MaterialShowcaseSequence(activity, "123")
        sequence.setConfig(config)
        var etSearchIntro: String = getString(R.string.et_search_intro)
        var i = 1
        for (s: String in searchOptionsValues) {
            etSearchIntro += "\n$i.$s"
            i++
        }
        val spSearchIntro = getString(R.string.sp_search_intro)
        val cbExactMatchIntro = getString(R.string.cb_exactMatch_intro)
        val btSearchIntro = getString(R.string.bt_search_intro)
        sequence.addSequenceItem(
            binding.etSearch,
            etSearchIntro, getString(R.string.got_it)
        )
        sequence.addSequenceItem(
            binding.spSearch,
            spSearchIntro, getString(R.string.next)
        )
        sequence.addSequenceItem(
            binding.cbExactMatch,
            cbExactMatchIntro, getString(R.string.next)
        )
        sequence.addSequenceItem(
            binding.btnSearch,
            btSearchIntro, getString(R.string.finish)
        )
        sequence.start()
    }

    override fun showSearchedResources(searchedEntities: MutableList<SearchedEntity>) {
        searchAdapter.setSearchResults(searchedEntities)
        this.searchedEntities = searchedEntities
        EspressoIdlingResource.decrement() // App is idle.
    }

    override fun showNoResultFound() {
        searchedEntities.clear()
        searchAdapter.notifyDataSetChanged()
        show(binding.etSearch, getString(R.string.no_search_result_found))
    }

    override fun showMessage(message: Int) {
        Toast.makeText(activity, getString(message), Toast.LENGTH_SHORT).show()
        EspressoIdlingResource.decrement() // App is idle.
    }

    override fun showProgressbar(b: Boolean) {
        if (b) {
            showMifosProgressDialog()
        } else {
            hideMifosProgressDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchPresenter.detachView()
    }

    override fun onPause() {
        //Fragment getting detached, keyboard if open must be hidden
        hideKeyboard(binding.etSearch)
        super.onPause()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent.id == R.id.sp_search) {
            resources = if (position == 0) {
                (searchOptionsValues[0] + "," + searchOptionsValues[1] + "," +
                        searchOptionsValues[2] + "," + searchOptionsValues[3] + "," +
                        searchOptionsValues[4])
            } else {
                searchOptionsValues[position - 1]
            }
            autoTriggerSearch = true
            onClickSearch()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    /**
     * There is a need for this method in the following cases :
     *
     *
     * 1. If user entered a search query and went out of the app.
     * 2. If user entered a search query and got some search results and went out of the app.
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            val queryString = binding.etSearch.editableText.toString()
            if (queryString != "") {
                outState.putString(LOG_TAG + binding.etSearch.id, queryString)
            }
        } catch (npe: NullPointerException) {
            //Looks like edit text didn't get initialized properly
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            val queryString = savedInstanceState.getString(LOG_TAG + binding.etSearch.id)
            if (!TextUtils.isEmpty(queryString)) {
                binding.etSearch.setText(queryString)
            }
        }
    }

    private fun onClickSearch() {
        hideKeyboard(binding.etSearch)
        val query = binding.etSearch.editableText.toString().trim { it <= ' ' }
        if (query.isNotEmpty()) {
            EspressoIdlingResource.increment() // App is busy until further notice.
            searchPresenter.searchResources(query, resources, binding.cbExactMatch.isChecked)
        } else {
            if (!autoTriggerSearch) {
                show(binding.etSearch, getString(R.string.no_search_query_entered))
            }
        }
    }

    companion object {
        private val LOG_TAG = SearchFragment::class.java.simpleName
    }
}