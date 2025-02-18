package com.auth0.guardian.sample.fragments.consent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth0.guardian.sample.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConsentPaymentInitiationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConsentPaymentInitiationFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String BINDING_MESSAGE = "binding_message";
    private static final String REMITTANCE_INFO = "remittance_info";
    private static final String CREDITOR_ACCOUNT = "creditor_account";
    private static final String AMOUNT_CURRENCY = "amount_currency";
    private static final String AMOUNT = "amount";

    private TextView bindingMessageText;
    private TextView remittanceInfoText;
    private TextView creditorAccountText;
    private TextView amountCurrencyText;
    private TextView amountText;

    private String bindingMessage;
    private String remittanceInfo;
    private String creditorAccount;
    private String amountCurrency;
    private String amount;

    public ConsentPaymentInitiationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bindingMessage Transaction binding message
     * @param remittanceInfo Payment remittance info.
     * @param creditorAccount Payment creditor account.
     * @param amountCurrency Payment amount currencty.
     * @param amount Payment amount
     *
     * @return A new instance of fragment ConsentPaymentInitiationFragment.
     */
    public static ConsentPaymentInitiationFragment newInstance(String bindingMessage,
                                                               String remittanceInfo,
                                                               String creditorAccount,
                                                               String amountCurrency,
                                                               String amount) {
        ConsentPaymentInitiationFragment fragment = new ConsentPaymentInitiationFragment();
        Bundle args = new Bundle();
        args.putString(BINDING_MESSAGE, bindingMessage);
        args.putString(REMITTANCE_INFO, remittanceInfo);
        args.putString(CREDITOR_ACCOUNT, creditorAccount);
        args.putString(AMOUNT_CURRENCY, amountCurrency);
        args.putString(AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bindingMessage = getArguments().getString(BINDING_MESSAGE);
            remittanceInfo = getArguments().getString(REMITTANCE_INFO);
            creditorAccount = getArguments().getString(CREDITOR_ACCOUNT);
            amountCurrency = getArguments().getString(AMOUNT_CURRENCY);
            amount = getArguments().getString(AMOUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_consent_payment_initiation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingMessageText = view.findViewById(R.id.bindingMessage);
        remittanceInfoText = view.findViewById(R.id.paymentRemittanceInformationText);
        creditorAccountText = view.findViewById(R.id.paymentAccountText);
        amountCurrencyText = view.findViewById(R.id.paymentAmountCurrencyText);
        amountText = view.findViewById(R.id.paymentAmountText);

        updateUI();
    }

    private void updateUI() {
        bindingMessageText.setText(bindingMessage);
        remittanceInfoText.setText(remittanceInfo);
        creditorAccountText.setText(creditorAccount);
        amountCurrencyText.setText(amountCurrency);
        amountText.setText(amount);
    }
}