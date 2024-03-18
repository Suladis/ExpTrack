package com.example.exptrack.ui.writenfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.exptrack.R;
import java.io.IOException;

public class WriteFragment extends Fragment {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private Context context;

    private EditText et1;
    private Button b1;
    private TextView tv1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireContext();
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            Toast.makeText(context, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            requireActivity().finish();
        }
        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_write, container, false);
        et1 = root.findViewById(R.id.et1);
        tv1 = root.findViewById(R.id.tv3);
        b1 = root.findViewById(R.id.b1);

        b1.setOnClickListener(v -> {
            String textToWrite = et1.getText().toString().trim();
            if (!textToWrite.isEmpty()) {
                Toast.makeText(context, "Please scan an NFC tag.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Please enter text to write to NFC tag.", Toast.LENGTH_LONG).show();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(requireActivity(), pendingIntent, null, null);
        processNfcIntent(requireActivity().getIntent());
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(requireActivity());
    }

    private void processNfcIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String tagText = readTextFromTag(tag);
                tv1.setText(tagText);
            }
        }
    }

    private String readTextFromTag(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage message = ndef.getNdefMessage();
                if (message != null && message.getRecords().length > 0) {
                    NdefRecord record = message.getRecords()[0];
                    return new String(record.getPayload());
                }
            } catch (IOException | FormatException e) {
                e.printStackTrace();
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "No text found on NFC tag.";
    }

}
