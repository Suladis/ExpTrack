package com.example.exptrack;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class WriteNFC extends Activity implements NfcAdapter.ReaderCallback {

    EditText edit_message;
    TextView nfc_contents;
    Button activateButton;
    Tag tag;
    boolean isWriteMode = false;
    String stringToWrite = "";

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writetag);
        edit_message = findViewById(R.id.et1);
        nfc_contents = findViewById(R.id.tv3);
        activateButton = findViewById(R.id.b1);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show();
        }

        activateButton.setOnClickListener(v -> {
            if (!isWriteMode) {
                stringToWrite = edit_message.getText().toString();
                if (stringToWrite.isEmpty()) {
                    Toast.makeText(this, "Enter text to write to NFC tag", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Bring NFC tag close to write the message", Toast.LENGTH_SHORT).show();
                    isWriteMode = true;
                }
            } else {
                    // Write to tag only if tag is detected and isWriteMode is true
                    if (tag != null && stringToWrite != null && !stringToWrite.isEmpty()) {
                    isWriteMode = false;
                    }
                }
        });

        // Register NFC setting change receiver
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
    }



    @Override
    protected void onResume() {
        super.onResume();

        if(nfcAdapter!= null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            nfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    private void enableNfc() {
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // Enable reader mode to listen for NFC tags
            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        } else {
            Toast.makeText(this, "Enable your NFC capabilities", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onTagDiscovered(Tag tag) {

        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type
        Ndef mNdef = Ndef.get(tag);

        // Check that it is an Ndef capable card
        if (mNdef != null) {

            // If we want to read
            // As we did not turn on the NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            // We can get the cached Ndef message the system read for us.

            NdefMessage mNdefMessage = mNdef.getCachedNdefMessage();
            if (mNdefMessage != null) {
                String messageAsString = ndefMessageToString(mNdefMessage);
                nfc_contents.setText(messageAsString);
            } else {
                nfc_contents.setText("No Contents");
            }

            if (isWriteMode) {
                // Or if we want to write a Ndef message
                // Create a Ndef Record
                NdefRecord mRecord = NdefRecord.createTextRecord("en", edit_message.getText().toString());

                // Add to a NdefMessage
                NdefMessage mMsg = new NdefMessage(mRecord);

                // Catch errors
                try {
                    mNdef.connect();
                    mNdef.writeNdefMessage(mMsg);

                    // Success if got to here
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                            "Write to NFC Success",
                            Toast.LENGTH_SHORT).show());

                    // Make a Sound
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                                notification);
                        r.play();
                    } catch (Exception e) {
                        // Some error playing sound
                    }

                } catch (FormatException e) {
                    // if the NDEF Message to write is malformed
                } catch (TagLostException e) {
                    // Tag went out of range before operations were complete
                } catch (IOException e) {
                    // if there is an I/O failure, or the operation is cancelled
                } catch (SecurityException e) {
                    // The SecurityException is only for Android 12L and above
                    // The Tag object might have gone stale by the time
                    // the code gets to process it, with a new one been
                    // delivered (for the same or different Tag)
                    // The SecurityException is thrown if you are working on
                    // a stale Tag
                } finally {
                    // Be nice and try and close the tag to
                    // Disable I/O operations to the tag from this TagTechnology object, and release resources.
                    try {
                        mNdef.close();
                    } catch (IOException e) {
                        // if there is an I/O failure, or the operation is cancelled
                    }
                }
                isWriteMode = false;
            }
        }
    }


    private String ndefMessageToString(NdefMessage message) {
        if (message == null)
            return "";

        NdefRecord[] records = message.getRecords();
        StringBuilder sb = new StringBuilder();
        for (NdefRecord record : records) {
            sb.append(new String(record.getPayload()));
        }
        return sb.toString();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            assert action != null;
            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);
                switch (state) {
                    case NfcAdapter.STATE_OFF:
                        // Tell the user to turn NFC on if App requires it
                        break;
                    case NfcAdapter.STATE_TURNING_OFF:
                        break;
                    case NfcAdapter.STATE_ON:
                        enableNfc();
                        break;
                    case NfcAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };
}
