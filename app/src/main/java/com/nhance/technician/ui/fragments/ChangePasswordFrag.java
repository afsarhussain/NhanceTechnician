package com.nhance.technician.ui.fragments;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.nhance.technician.R;
import com.nhance.technician.app.ApplicationConstants;
import com.nhance.technician.app.NhanceApplication;
import com.nhance.technician.datasets.GeneratedInvoiceTable;
import com.nhance.technician.exception.NhanceException;
import com.nhance.technician.logger.LOG;
import com.nhance.technician.model.Application;
import com.nhance.technician.model.MasterDataDTO;
import com.nhance.technician.model.SellerLoginDTO;
import com.nhance.technician.model.ServiceRequestInvoiceDTO;
import com.nhance.technician.networking.RestCall;
import com.nhance.technician.networking.json.JSONAdaptor;
import com.nhance.technician.networking.util.RestConstants;
import com.nhance.technician.ui.AlertCode;
import com.nhance.technician.ui.BaseFragmentActivity;
import com.nhance.technician.ui.TechOperationsActivity;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChangePasswordFrag extends Fragment implements ApplicationConstants {

    public static final String TAG = ChangePasswordFrag.class.getName();
    private View v;

    private TextInputLayout newPswdInputLay,current_pswd_input_lay;
    private AppCompatEditText newPswdeET, input_current_password;

    Button forceChangePasswordBtn;

    private CoordinatorLayout coordinatorLayout;

    public ChangePasswordFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        /*getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {*/
                if(show){
                    ((BaseFragmentActivity)getActivity()).showProgressDialog(getActivity(), "");
                }else{
                    ((BaseFragmentActivity)getActivity()).dismissProgressDialog();
                }
            /*}
        });*/
    }

    private void initViews() {
        coordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.coordinatorLayout);

        newPswdInputLay = (TextInputLayout) v.findViewById(R.id.new_pswd_input_lay);
        newPswdeET = (AppCompatEditText) v.findViewById(R.id.input_new_password);

        btn_layout = (LinearLayout)v.findViewById(R.id.btn_layout);

        current_pswd_input_lay = (TextInputLayout) v.findViewById(R.id.current_pswd_input_lay);
        input_current_password = (AppCompatEditText) v.findViewById(R.id.input_current_password);

        forceChangePasswordBtn = (Button) v.findViewById(R.id.force_set_pswd_bttn);

        newPswdeET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                newPswdInputLay.setError(null);
                newPswdInputLay.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        input_current_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                current_pswd_input_lay.setError(null);
                current_pswd_input_lay.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private SellerLoginDTO sellerLoginDTO = null;

    private void syncAndFetchStoredInvoices(String userCode) {
        showProgress(true);
        try {

            Callback call = new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showProgress(false);
                    ((BaseFragmentActivity)getActivity()).showAlert("Unable to process your request. Please try again.");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    showProgress(false);
                    if (response.isSuccessful()) {
                        int responseCode = response.code();
                        if (responseCode == 200) {
                            try {
                                String resStr = response.body().string();
                                LOG.d("UserLoginTask", resStr);
                                MasterDataDTO masterDataDTO = JSONAdaptor.fromJSON(resStr, MasterDataDTO.class);

                                if (masterDataDTO != null) {
                                    int status = 0;
                                    if (masterDataDTO.getResponseStatus() != null) {
                                        status = masterDataDTO.getResponseStatus();
                                    }
                                    if (status > 0) {
                                        String errorMsg = masterDataDTO.getMessageDescription();
                                        ((BaseFragmentActivity)getActivity()).showAlert(errorMsg);
                                    } else {

                                        List<ServiceRequestInvoiceDTO> serviceRequestInvoiceDTOList = masterDataDTO.getInvoiceHistory();
                                        if (serviceRequestInvoiceDTOList != null && serviceRequestInvoiceDTOList.size() > 0) {
                                            LOG.d(TAG, serviceRequestInvoiceDTOList.toString());
                                            try {
                                                new GeneratedInvoiceTable().storeServiceReqInvoiceDetails(serviceRequestInvoiceDTOList);
                                            } catch (Exception e) {

                                            }
                                        }
                                        moveToDashboard();
                                    }
                                } else {
                                    ((BaseFragmentActivity)getActivity()).showAlert( getResources().getString(R.string.unable_to_process));
                                }
                            } catch (IOException ioe) {
                                ((BaseFragmentActivity)getActivity()).showAlert("Unable to process your request. Please try again.");
                            } catch (NhanceException e) {
                                ((BaseFragmentActivity)getActivity()).showAlert("Unable to process your request. Please try again.");
                            }
                        } else if (responseCode == 404 || responseCode == 503) {
                            LOG.d(TAG, "Server Unreachable. Please try after some time");
                            ((BaseFragmentActivity)getActivity()).showAlert("Server Unreachable. Please try after some time");
                        } else if (responseCode == 500) {
                            LOG.d(TAG, "Internal server error.");
                            ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                        } else {
                            ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                        }

                    } else {
                        ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                    }
                }

            };
            ServiceRequestInvoiceDTO serviceRequestInvoiceDTO = new ServiceRequestInvoiceDTO();
            serviceRequestInvoiceDTO.setUserCode(userCode);
            serviceRequestInvoiceDTO.setLastSyncTime(0L);
            serviceRequestInvoiceDTO.setDefaultLocale("en_US");
            LOG.d("Request===> ", serviceRequestInvoiceDTO.toString());
            RestCall.post(NhanceApplication.getApplication().getBackendUrl() + RestConstants.SYNC_REQ, JSONAdaptor.toJSON(serviceRequestInvoiceDTO), call);
        } catch (IOException e) {
            e.printStackTrace();
            ((BaseFragmentActivity)getActivity()).showAlert(e.getLocalizedMessage());
        } catch (NhanceException e) {
            e.printStackTrace();
            ((BaseFragmentActivity)getActivity()).showAlert(e.getLocalizedMessage());
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptResetPassword() {

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true);
            /*mAuthTask = new UserLoginTask(mobileNo, password);
            mAuthTask.execute((Void) null);*/
        try {

            Callback call = new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showProgress(false);
                    sellerLoginDTO = null;
                    sellerLoginDTO = new SellerLoginDTO();
                    sellerLoginDTO.setResponseStatus(1);
                    sellerLoginDTO.setMessageDescription("Unable to process your request. Please try again.");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    showProgress(false);
                    if (response.isSuccessful()) {
                        int responseCode = response.code();
                            /*Intent mainIntent = new Intent(LoginActivity.this, TechOperationsActivity.class);
                            startActivity(mainIntent);
                            finish();*/
                        if (responseCode == 200) {
                            try {
                                String resStr = response.body().string();
                                LOG.d("UserLoginTask", resStr);
                                sellerLoginDTO = JSONAdaptor.fromJSON(resStr, SellerLoginDTO.class);

                                if (sellerLoginDTO != null) {
                                    int status = 0;
                                    if (sellerLoginDTO.getResponseStatus() != null) {
                                        status = sellerLoginDTO.getResponseStatus();
                                    }
                                    if (status > 0) {
                                        String errorMsg = sellerLoginDTO.getMessageDescription();
                                        ((BaseFragmentActivity)getActivity()).showAlert(errorMsg);
                                    } else {
                                        LOG.d("", sellerLoginDTO.toString());
                                        ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.password_changed_successfully), getResources().getString(R.string.ok), null, AlertCode.CHANGE_PASSWORD_SUCCESS);
                                    }
                                } else {
                                    ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.unable_to_process));
                                }
                            } catch (IOException ioe) {
                                ((BaseFragmentActivity)getActivity()).showAlert("Server Unreachable. Please try after some time.");
                            } catch (NhanceException e) {
                                ((BaseFragmentActivity)getActivity()).showAlert("Server Unreachable. Please try after some time.");
                            }
                        } else if (responseCode == 404 || responseCode == 503) {
                            LOG.d(TAG, "Server Unreachable. Please try after some time");
                            ((BaseFragmentActivity)getActivity()).showAlert("Server Unreachable. Please try after some time.");
                        } else if (responseCode == 500) {
                            LOG.d(TAG, "Internal server error.");
                            ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                        } else {
                            ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                        }

                    } else {
                        ((BaseFragmentActivity)getActivity()).showAlert("Error while communicating with server, please contact administrator.");
                    }
                }

            };

            String oldPassword = input_current_password.getText().toString().trim();
            String newPassword = newPswdeET.getText().toString().trim();

            sellerLoginDTO = new SellerLoginDTO();
            sellerLoginDTO.setOldPassword(oldPassword);
            sellerLoginDTO.setPassword(newPassword);
            sellerLoginDTO.setMobileNumber(Application.getInstance().getMobileNumber());
            sellerLoginDTO.setIsdCode("91");
            sellerLoginDTO.setDefaultLocale("en_US");
            sellerLoginDTO.setAppType(Application.getInstance().getApplicationType());
            LOG.d("Request===> ", sellerLoginDTO.toString());
            RestCall.post(NhanceApplication.getApplication().getBackendUrl() + RestConstants.CHANGE_PASSWORD_URL, JSONAdaptor.toJSON(sellerLoginDTO), call);
        } catch (IOException e) {
            e.printStackTrace();
            ((BaseFragmentActivity)getActivity()).showAlert("Unable to process your request. Please try again.");
        } catch (NhanceException e) {
            e.printStackTrace();
            ((BaseFragmentActivity)getActivity()).showAlert("Unable to process your request. Please try again.");
        }
    }

    private LinearLayout btn_layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_change_password, container, false);

        initViews();

        forceChangePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((BaseFragmentActivity) getActivity()).hideSoftKeyPad();

                validateAndProceed();
            }
        });

        return v;
    }

    /*private void showAlert(String message){
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }*/

    private void validateAndProceed(){

        if (((BaseFragmentActivity)getActivity()).getmSystemService().getActiveNetworkInfo() == null) {
            ((BaseFragmentActivity)getActivity()).showAlert(getString(R.string.network_error));
            return;
        }

        if(!isDataValid())
        {
            return;
        }

        attemptResetPassword();
    }

    private boolean isDataValid() {

        boolean retValue = true;

        View focusView = null;

        if(input_current_password.getText().toString().trim().length() == 0 ) {
            ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.please_enter_old_password));
            retValue = false;
            focusView = input_current_password;
        }
        else if(input_current_password.getText().toString().trim().length() >0 && input_current_password.getText().toString().trim().length()<6 ) {
            ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.password_should_be_of_length));
            retValue = false;
            focusView = input_current_password;
        }
        else if(newPswdeET.getText().toString().trim().length() == 0 ) {
            ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.please_enter_new_password));
            retValue = false;
            focusView = newPswdeET;
        }
        else if(newPswdeET.getText().toString().trim().length() >0 && newPswdeET.getText().toString().trim().length()<6 ) {
            ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.password_should_be_of_length));
            retValue = false;
            focusView = newPswdeET;
        }
        else if((newPswdeET.getText().toString().trim().length() > 0 && input_current_password.getText().toString().trim().length() > 0) && (newPswdeET.getText().toString().trim().equals(input_current_password.getText().toString().trim()))) {
            ((BaseFragmentActivity)getActivity()).showAlert(getResources().getString(R.string.old_password_and_new_password_should_not_be_same));
            retValue = false;
            focusView = newPswdeET;
        }

        if(focusView != null){
            focusView.requestFocus();
        }

        return retValue;
    }

    private void moveToDashboard(){

        Intent mainIntent = new Intent(getActivity(), TechOperationsActivity.class);
        mainIntent.putExtra("USERCODE", sellerLoginDTO.getUserCode());
        getActivity().startActivity(mainIntent);
        getActivity().finish();
    }
}
