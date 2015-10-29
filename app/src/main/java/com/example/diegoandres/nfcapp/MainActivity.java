package com.example.diegoandres.nfcapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter myNfcAdapter;
    private ToggleButton botonLeer;
    private ToggleButton botonEscribir;
    private TextView txtResultado;
    private TextView txtId;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        botonLeer = (ToggleButton) findViewById(R.id.botonLeer);
        botonEscribir = (ToggleButton) findViewById(R.id.botonEscribir);
        txtResultado = (TextView) findViewById(R.id.textViewResultado);
        txtId = (TextView) findViewById(R.id.textViewIdTag);

        if (myNfcAdapter == null) {
            // El celular no tiene NFC..

            Toast.makeText(this, "Este dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!myNfcAdapter.isEnabled()) {
            // Decirle al Usuario que active el NGC
            Toast.makeText(this, "El NFC está desactivado. Por favor active el NFC", Toast.LENGTH_LONG).show();
        } else {
            // El NFC está activado...
            onNewIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
        onNewIntent(getIntent());
    }


    @Override
    protected void onPause() {
        super.onPause();
        myNfcAdapter.disableForegroundDispatch(this);
    }


    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        i = new Intent();
        i = intent;


        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {

            Tag myTagId = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String id = bytesToHexString(myTagId.getId());
            txtId.setText(" " + id);

            botonEscribir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Se crea el objeto tipo Tag llamado myTag
                    Tag myTag = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                    //Mensaje que deseo almacenar en el Tag.
                    String[] almacenar = new String[]{"Hola", "Jota", "Mario"};
                    NdefMessage ndefMessage = createNdefMessage(almacenar);
                    writeNdefMessage(myTag, ndefMessage);
                    Toast.makeText(MainActivity.this, "Se escribió el Tag", Toast.LENGTH_SHORT).show();

                }
            });

            botonLeer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Parcelable[] parcelables = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if (parcelables != null && parcelables.length > 0) {
                        //Se leen los NdefMessages del Tag
                        readTextFromMessage((NdefMessage) parcelables[0]);
                    } else {
                        Toast.makeText(MainActivity.this, "No NDEF mensajes encontrados", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }


    private void readTextFromMessage(NdefMessage ndefMessage) {

        //Se obtienen los records almacenados en el ndefMessage
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        //En este String se concatenarán los mensajes extraídos de los records
        String tagContentFull = "";

        if (ndefRecords != null && ndefRecords.length > 0) {
            //Existen ndefRecords almacenados

            //Número de Récords almacenados
            int nroRecords = ndefRecords.length;

            for (int i = 0; i < nroRecords; i++) {

                NdefRecord ndefRecord = ndefRecords[i];
                String tagContent = getTextFromNdefRecord(ndefRecord);
                tagContentFull = (tagContentFull + "\r\n" + tagContent);
            }

            //Se muestra el contenido del Tag
            txtResultado.setText(tagContentFull);

        } else {
            Toast.makeText(this, "¡No NDEF mensajes encontrados!", Toast.LENGTH_SHORT).show();
        }
    }


    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        myNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);

    }


    private void disableForegroundDispatchSystem() {

        myNfcAdapter.disableForegroundDispatch(this);

    }


    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not ndef formatable", Toast.LENGTH_SHORT).show();
                return;
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {

            Log.e("formatTag", e.getMessage());
        }

    }


    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {

            if (tag == null) {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                //Formato del Tag con ndef Formato y se escribe el mensaje
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {

            Log.e("WriteNdefMessage", e.getMessage());

        }

    }


    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {

            Log.e("CreateTextRecord", e.getMessage());

        }

        return null;
    }

    private NdefMessage createNdefMessage(String[] content) {


        int size = content.length;
        NdefRecord[] arreglo = new NdefRecord[size];


        for(int i = 0; i<size; i++){

        NdefRecord ndefRecord = createTextRecord(content[i]);
        arreglo[i] = ndefRecord;
        }

        NdefMessage ndefMessage = new NdefMessage(arreglo);

        return ndefMessage;
    }


    public String getTextFromNdefRecord(NdefRecord ndefRecord) {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }
        return tagContent;
    }


    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

}
