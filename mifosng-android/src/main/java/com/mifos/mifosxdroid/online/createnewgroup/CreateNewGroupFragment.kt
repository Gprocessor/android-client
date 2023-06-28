/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */
package com.mifos.mifosxdroid.online.createnewgroup

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.mifos.exceptions.InvalidTextInputException
import com.mifos.exceptions.RequiredFieldException
import com.mifos.exceptions.ShortOfLengthException
import com.mifos.mifosxdroid.R
import com.mifos.mifosxdroid.core.MifosBaseActivity
import com.mifos.mifosxdroid.core.ProgressableFragment
import com.mifos.mifosxdroid.core.util.Toaster
import com.mifos.mifosxdroid.databinding.FragmentCreateNewGroupBinding
import com.mifos.mifosxdroid.online.GroupsActivity
import com.mifos.mifosxdroid.uihelpers.MFDatePicker
import com.mifos.mifosxdroid.uihelpers.MFDatePicker.OnDatePickListener
import com.mifos.objects.group.GroupPayload
import com.mifos.objects.organisation.Office
import com.mifos.objects.response.SaveResponse
import com.mifos.utils.Constants
import com.mifos.utils.DateHelper
import com.mifos.utils.FragmentConstants
import com.mifos.utils.MifosResponseHandler
import com.mifos.utils.Network
import com.mifos.utils.PrefManager
import com.mifos.utils.ValidationUtil
import javax.inject.Inject

/**
 * Created by nellyk on 1/22/2016.
 */ //TODO Show Image and Text after successful or Failed during creation of Group and
//TODO A button to Continue or Finish the GroupCreation.
class CreateNewGroupFragment : ProgressableFragment(), OnDatePickListener, CreateNewGroupMvpView,
    OnItemSelectedListener {

    private lateinit var binding: FragmentCreateNewGroupBinding

    private val LOG_TAG = javaClass.simpleName

    @Inject
    lateinit var mCreateNewGroupPresenter: CreateNewGroupPresenter
    var activationDateString: String? = null
    var officeId = 0
    var result = true
    var dateofsubmissionstring: String? = null
    private var mfDatePicker: DialogFragment? = null
    private var newDatePicker: DialogFragment? = null
    private val mListOffices: MutableList<String> = ArrayList()
    private var officeList: List<Office>? = null
    private var mOfficesAdapter: ArrayAdapter<String>? = null
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (parent.id == R.id.sp_group_offices) {
            officeId = officeList!![position].id
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as MifosBaseActivity).activityComponent?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateNewGroupBinding.inflate(inflater, container, false)
        mCreateNewGroupPresenter.attachView(this)
        inflateOfficesSpinner()
        inflateSubmissionDate()
        inflateActivationDate()
        mCreateNewGroupPresenter.loadOffices()

        //client active checkbox onCheckedListener

        activationDateString = binding.tvGroupActivationDate.text.toString()
        activationDateString =
            DateHelper.getDateAsStringUsedForCollectionSheetPayload(activationDateString)
                .replace("-", " ")
        dateofsubmissionstring = binding.tvGroupSubmissionDate.text.toString()
        dateofsubmissionstring =
            DateHelper.getDateAsStringUsedForDateofBirth(dateofsubmissionstring).replace("-", " ")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmit.setOnClickListener {
            if (Network.isOnline(requireContext())) {
                val groupPayload = GroupPayload()
                groupPayload.name = binding.etGroupName.editableText.toString()
                groupPayload.externalId = binding.etGroupExternalId.editableText.toString()
                groupPayload.isActive = binding.cbGroupActiveStatus.isChecked
                groupPayload.activationDate = activationDateString
                groupPayload.setSubmissionDate(dateofsubmissionstring)
                groupPayload.officeId = officeId
                groupPayload.dateFormat = "dd MMMM yyyy"
                groupPayload.locale = "en"
                initiateGroupCreation(groupPayload)
            } else {
                Toaster.show(binding.root, R.string.error_network_not_available, Toaster.LONG)
            }
        }

        binding.cbGroupActiveStatus.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.layoutSubmission.visibility = View.VISIBLE
            } else {
                binding.layoutSubmission.visibility = View.GONE
            }
        }
    }

    private fun initiateGroupCreation(groupPayload: GroupPayload) {
        //TextField validations
        if (!isGroupNameValid) {
            return
        }
        mCreateNewGroupPresenter.createGroup(groupPayload)
    }

    private fun inflateOfficesSpinner() {
        mOfficesAdapter = ArrayAdapter(
            requireActivity(), android.R.layout.simple_spinner_item,
            mListOffices
        )
        mOfficesAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spGroupOffices.adapter = mOfficesAdapter
        binding.spGroupOffices.onItemSelectedListener = this
    }

    private fun inflateSubmissionDate() {
        mfDatePicker = MFDatePicker.newInsance(this)
        binding.tvGroupSubmissionDate.text = MFDatePicker.datePickedAsString
        binding.tvGroupSubmissionDate.setOnClickListener {
            (mfDatePicker as MFDatePicker?)?.show(
                requireActivity().supportFragmentManager,
                FragmentConstants.DFRAG_DATE_PICKER
            )
        }
    }

    private fun inflateActivationDate() {
        newDatePicker = MFDatePicker.newInsance(this)
        binding.tvGroupActivationDate.text = MFDatePicker.datePickedAsString
        binding.tvGroupActivationDate.setOnClickListener {
            (newDatePicker as MFDatePicker?)?.show(
                requireActivity().supportFragmentManager,
                FragmentConstants.DFRAG_DATE_PICKER
            )
        }
    }

    override fun onDatePicked(date: String?) {
        binding.tvGroupSubmissionDate.text = date
        binding.tvGroupActivationDate.text = date
    }

    private val isGroupNameValid: Boolean
        get() {
            result = true
            try {
                if (TextUtils.isEmpty(binding.etGroupName.editableText.toString())) {
                    throw RequiredFieldException(
                        resources.getString(R.string.group_name),
                        resources.getString(R.string.error_cannot_be_empty)
                    )
                }
                if (binding.etGroupName.editableText.toString()
                        .trim { it <= ' ' }.length < 4 && binding.etGroupName
                        .editableText.toString().trim { it <= ' ' }.isNotEmpty()
                ) {
                    throw ShortOfLengthException(resources.getString(R.string.group_name), 4)
                }
                if (!ValidationUtil.isNameValid(binding.etGroupName.editableText.toString())) {
                    throw InvalidTextInputException(
                        resources.getString(R.string.group_name),
                        resources.getString(R.string.error_should_contain_only),
                        InvalidTextInputException.TYPE_ALPHABETS
                    )
                }
            } catch (e: InvalidTextInputException) {
                e.notifyUserWithToast(activity)
                result = false
            } catch (e: ShortOfLengthException) {
                e.notifyUserWithToast(activity)
                result = false
            } catch (e: RequiredFieldException) {
                e.notifyUserWithToast(activity)
                result = false
            }
            return result
        }

    override fun showOffices(offices: List<Office?>?) {
        officeList = offices as List<Office>?
        if (offices != null) {
            for (office in offices) {
                mListOffices.add(office.name)
            }
        }
        mListOffices.sort()
        mOfficesAdapter?.notifyDataSetChanged()
    }

    override fun showGroupCreatedSuccessfully(group: SaveResponse?) {
        Toast.makeText(
            activity, "Group " + MifosResponseHandler.getResponse(),
            Toast.LENGTH_LONG
        ).show()
        requireActivity().supportFragmentManager.popBackStack()
        if (PrefManager.userStatus == Constants.USER_ONLINE) {
            val groupActivityIntent = Intent(activity, GroupsActivity::class.java)
            groupActivityIntent.putExtra(Constants.GROUP_ID, group?.groupId)
            startActivity(groupActivityIntent)
        }
    }

    override fun showFetchingError(s: String?) {
        Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
    }

    override fun showProgressbar(b: Boolean) {
        showProgress(b)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCreateNewGroupPresenter.detachView()
    }

    companion object {
        @JvmStatic
        fun newInstance(): CreateNewGroupFragment {
            return CreateNewGroupFragment()
        }
    }
}