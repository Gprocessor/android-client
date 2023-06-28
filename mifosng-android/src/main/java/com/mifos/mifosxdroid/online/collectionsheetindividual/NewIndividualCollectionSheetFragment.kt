package com.mifos.mifosxdroid.online.collectionsheetindividual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.mifos.api.model.RequestCollectionSheetPayload
import com.mifos.mifosxdroid.R
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.core.MifosBaseFragment
import com.mifos.mifosxdroid.core.util.Toaster
import com.mifos.mifosxdroid.databinding.FragmentNewCollectionSheetBinding
import com.mifos.mifosxdroid.dialogfragments.collectionsheetdialog.CollectionSheetDialogFragment
import com.mifos.mifosxdroid.dialogfragments.searchdialog.SearchDialog
import com.mifos.mifosxdroid.online.collectionsheetindividualdetails.IndividualCollectionSheetDetailsFragment
import com.mifos.mifosxdroid.uihelpers.MFDatePicker
import com.mifos.mifosxdroid.uihelpers.MFDatePicker.OnDatePickListener
import com.mifos.mifosxdroid.views.CustomSpinner.OnSpinnerEventsListener
import com.mifos.objects.collectionsheet.IndividualCollectionSheet
import com.mifos.objects.organisation.Office
import com.mifos.objects.organisation.Staff
import com.mifos.utils.Constants
import com.mifos.utils.DateHelper
import com.mifos.utils.FragmentConstants
import javax.inject.Inject


/**
 * Created by aksh on 18/6/18.
 */
class NewIndividualCollectionSheetFragment : MifosBaseFragment(), IndividualCollectionSheetMvpView,
    OnDatePickListener, OnItemSelectedListener, View.OnClickListener {

    private lateinit var binding: FragmentNewCollectionSheetBinding

    @Inject
    lateinit var presenter: NewIndividualCollectionSheetPresenter
    private var sheet: IndividualCollectionSheet? = null
    private var datePicker: DialogFragment? = null
    private var requestPayload: RequestCollectionSheetPayload? = null
    private var officeAdapter: ArrayAdapter<String>? = null
    private lateinit var officeNameList: ArrayList<String>
    private var officeList: List<Office> = ArrayList()
    private var staffAdapter: ArrayAdapter<String>? = null
    private lateinit var staffNameList: ArrayList<String>
    private var staffList: List<Staff> = ArrayList()
    private var officeId = 0
    private var staffId = 0
    private val requestCode = 1
    private var success = true
    private var actualDisbursementDate: String? = null
    private var transactionDate: String? = null

    private var officeSearchDialog: SearchDialog? = null
    private var staffSearchDialog: SearchDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as MifosBaseActivity).activityComponent?.inject(this)
        if (savedInstanceState != null) {
            sheet =
                savedInstanceState[Constants.EXTRA_COLLECTION_INDIVIDUAL] as IndividualCollectionSheet
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewCollectionSheetBinding.inflate(inflater, container, false)
        setToolbarTitle(getStringMessage(R.string.individual_collection_sheet))
        presenter.attachView(this)
        setUpUi()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClear.setOnClickListener {
            clear()
        }
    }

    private fun setUpUi() {
        setRepaymentDate()
        officeNameList = ArrayList()
        officeAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_item, officeNameList
        )
        officeAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spOfficeList.adapter = officeAdapter
        binding.spOfficeList.onItemSelectedListener = this
        staffNameList = ArrayList()
        staffAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_item, staffNameList
        )
        staffAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spStaffList.adapter = staffAdapter
        binding.tvRepaymentDate.setOnClickListener(this)
        binding.btnFetchCollectionSheet.setOnClickListener(this)
        presenter.fetchOffices()

        binding.spOfficeList.setSpinnerEventsListener(object : OnSpinnerEventsListener {
            override fun onSpinnerOpened(spinner: Spinner, isItemListLarge: Boolean) {
                if (isItemListLarge) {
                    enableOfficeSearch()
                }
            }

            override fun onSpinnerClosed(spinner: Spinner) {}
        })

        binding.spStaffList.setSpinnerEventsListener(object : OnSpinnerEventsListener {
            override fun onSpinnerOpened(spinner: Spinner, isItemListLarge: Boolean) {
                if (isItemListLarge) {
                    enableStaffSearch()
                }
            }

            override fun onSpinnerClosed(spinner: Spinner) {}
        })

    }

    fun enableOfficeSearch() {
        if (officeSearchDialog == null) {
            val listener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
                binding.spOfficeList.setSelection(i)
            }
            officeSearchDialog = SearchDialog(requireContext(), officeNameList, listener)
        }
        officeSearchDialog?.show()
    }

    fun enableStaffSearch() {
        if (staffSearchDialog == null) {
            val listener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
                binding.spStaffList.setSelection(i)
            }
            staffSearchDialog = SearchDialog(requireContext(), staffNameList, listener)
        }
        staffSearchDialog?.show()
    }

    private fun setRepaymentDate() {
        datePicker = MFDatePicker.newInsance(this)
        val date =
            DateHelper.getDateAsStringUsedForCollectionSheetPayload(MFDatePicker.datePickedAsString)
        binding.tvRepaymentDate.text = date.replace('-', ' ')
        transactionDate = date.replace('-', ' ')
        actualDisbursementDate = transactionDate
    }

    private fun prepareRequestPayload() {
        requestPayload = RequestCollectionSheetPayload()
        requestPayload?.officeId = officeId
        requestPayload?.staffId = staffId
        requestPayload?.transactionDate = binding.tvRepaymentDate.text.toString()
    }

    override fun setOfficeSpinner(offices: List<Office>?) {
        if (offices != null) {
            officeList = offices
        }
        officeNameList.clear()
        officeNameList.add(getString(R.string.spinner_office))
        officeNameList.addAll(presenter.filterOffices(officeList))
        officeAdapter?.notifyDataSetChanged()
    }

    override fun onDatePicked(date: String?) {
        val d = DateHelper.getDateAsStringUsedForCollectionSheetPayload(date)
        binding.tvRepaymentDate.text = d.replace('-', ' ')
    }

    private fun retrieveCollectionSheet() {
        prepareRequestPayload()
        presenter.fetchIndividualCollectionSheet(requestPayload)
    }

    private fun setTvRepaymentDate() {
        datePicker?.show(
            requireActivity().supportFragmentManager,
            FragmentConstants.DFRAG_DATE_PICKER
        )
    }

    override fun setStaffSpinner(staffs: List<Staff>?) {
        binding.spStaffList.onItemSelectedListener = this
        if (staffs != null) {
            staffList = staffs
        }
        staffNameList.clear()
        staffNameList.add(getString(R.string.spinner_staff))
        staffNameList.addAll(presenter.filterStaff(staffList))
        staffAdapter?.notifyDataSetChanged()
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        when (adapterView.id) {
            R.id.sp_office_list -> if (i == officeList.size || i == 0) {
                Toaster.show(binding.root, getStringMessage(R.string.error_select_office))
            } else {
                Toaster.show(binding.root, officeNameList[i])
                officeId = officeList[i - 1].id
                presenter.fetchStaff(officeId)
            }

            R.id.sp_staff_list -> if (i == staffList.size || i == 0) {
                Toaster.show(binding.root, getStringMessage(R.string.error_select_staff))
            } else {
                staffId = staffList[i - 1].id
            }
        }
    }

    private fun popupDialog() {
        val collectionSheetDialogFragment = CollectionSheetDialogFragment.newInstance(
            binding.tvRepaymentDate.text.toString(),
            sheet?.clients?.size ?: 0
        )
        collectionSheetDialogFragment.setTargetFragment(this, requestCode)
        val fragmentTransaction = requireActivity().supportFragmentManager
            .beginTransaction()
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_DOCUMENT_LIST)
        collectionSheetDialogFragment.show(fragmentTransaction, "Identifier Dialog Fragment")
    }

    fun getResponse(response: String?) {
        when (response) {
            Constants.FILLNOW -> {
                val fm = activity
                    ?.supportFragmentManager
                fm?.popBackStack()
                val fragment: IndividualCollectionSheetDetailsFragment =
                    IndividualCollectionSheetDetailsFragment().newInstance(
                        sheet,
                        actualDisbursementDate, transactionDate
                    )
                (activity as MifosBaseActivity).replaceFragment(
                    fragment,
                    true, R.id.container
                )
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    override fun showSheet(individualCollectionSheet: IndividualCollectionSheet?) {
        sheet = individualCollectionSheet
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Constants.EXTRA_COLLECTION_INDIVIDUAL, sheet)
    }

    override fun showSuccess() {
        if (success) {
            popupDialog()
        }
    }

    override fun showError(message: String?) {
        Toaster.show(binding.root, message)
    }

    override fun showNoSheetFound() {
        success = false
        Toaster.show(binding.root, getStringMessage(R.string.no_collectionsheet_found))
    }

    override fun showProgressbar(b: Boolean) {
        if (b) {
            showMifosProgressDialog()
        } else {
            hideMifosProgressDialog()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.tv_repayment_date -> setTvRepaymentDate()
            R.id.btn_fetch_collection_sheet -> retrieveCollectionSheet()
        }
    }


    private fun clear() {
        binding.spOfficeList.adapter = null
        binding.spStaffList.adapter = null
        setUpUi()
    }

    companion object {
        fun newInstance(): NewIndividualCollectionSheetFragment {
            val args = Bundle()
            val fragment = NewIndividualCollectionSheetFragment()
            fragment.arguments = args
            return fragment
        }
    }
}