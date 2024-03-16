package com.example.exptrack.ui.writenfc;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.exptrack.R;
import com.example.exptrack.databinding.FragmentWriteBinding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class WriteFragment extends Fragment {

    private FragmentWriteBinding binding;
    private WriteViewModel writeViewModel;

    private static final String ERROR_DETECTED = "No NFC Tag Detected";
    private static final String WRITE_SUCCESS = "Tag has been coded";
    private static final String WRITE_ERROR = "Error Coding NFC";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] writeTagFilters;
    private boolean writeMode;
    private Tag myTag;
    private Context context;

    private TextView tv1;
    private EditText et1;
    private Button b1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        writeViewModel = new ViewModelProvider(this).get(WriteViewModel.class);
        binding = FragmentWriteBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Intent intent = requireActivity().getIntent();

        et1 = root.findViewById(R.id.et1);
        tv1 = root.findViewById(R.id.tv3);
        b1 = root.findViewById(R.id.b1);
        context = requireContext();

        b1.setOnClickListener(v -> {
            try {
                if (myTag == null) {
                    Toast.makeText(et1.getContext(), ERROR_DETECTED, Toast.LENGTH_LONG).show();
                } else {
                    // Get the text from the EditText
                    String textToWrite = et1.getText().toString();
                    // Write the text to the NFC tag
                    write(textToWrite, myTag);
                    Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG).show();
                }
            } catch (IOException | FormatException e) {
                Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });


        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            Toast.makeText(context, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            requireActivity().finish();
        }
        readFromIntent(requireActivity().getIntent());

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        pendingIntent = PendingIntent.getActivity(requireContext(), 0,
                new Intent(requireContext(), getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        writeTagFilters = new IntentFilter[] { tagDetected };

        return root;
    }



    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        tv1.setText("NFC Content: " + text);
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }



    public void handleNfcIntent(Intent intent) {
        // Handle NFC intent here
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // Handle the NFC intent accordingly
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }





    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }



    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
        // Automatically handle NFC intent
        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            handleNfcIntent(intent);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(requireActivity(), pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(requireActivity());
    }
}