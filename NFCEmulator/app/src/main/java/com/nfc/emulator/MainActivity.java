package com.nfc.emulator;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private Button btnRead, btnSave, btnEmulate;
    private TextView tvStatus;
    private Spinner spinnerCards;
    private CardStore store;

    private byte[] lastTagData;
    private String lastTagUid;
    private boolean readMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnRead = findViewById(R.id.btnRead);
        btnSave = findViewById(R.id.btnSave);
        btnEmulate = findViewById(R.id.btnEmulate);
        spinnerCards = findViewById(R.id.spinnerCards);
        store = new CardStore(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) { tvStatus.setText("NFC not available on this device."); return; }
        if (!nfcAdapter.isEnabled()) tvStatus.setText("Please enable NFC in settings.");

        pendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE);

        btnRead.setOnClickListener(v -> {
            readMode = true;
            tvStatus.setText("Tap a card to read...");
        });

        btnSave.setOnClickListener(v -> {
            if (lastTagData == null) return;
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Save Card");
            final EditText input = new EditText(this);
            input.setHint("Card name");
            b.setView(input);
            b.setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) name = "Card " + (store.getNames().size() + 1);
                store.save(new CardStore.Card(name, lastTagUid, bytesToHex(lastTagData)));
                tvStatus.setText("Saved: " + name);
                refreshSpinner();
            });
            b.setNegativeButton("Cancel", null);
            b.show();
        });

        btnEmulate.setOnClickListener(v -> {
            List<String> names = store.getNames();
            if (names.isEmpty()) { tvStatus.setText("No saved cards."); return; }
            spinnerCards.setVisibility(android.view.View.VISIBLE);
            int idx = spinnerCards.getSelectedItemPosition();
            CardStore.Card card = store.get(idx < 0 ? 0 : idx);
            if (card == null) return;
            HceService.cardData = hexToBytes(card.data);
            tvStatus.setText("Emulating: " + card.name + "\nHold phone to reader.");
        });

        refreshSpinner();
    }

    private void refreshSpinner() {
        List<String> names = store.getNames();
        if (!names.isEmpty()) {
            spinnerCards.setVisibility(android.view.View.VISIBLE);
            spinnerCards.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
            btnEmulate.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            IntentFilter[] filters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
            String[][] techLists = {
                {IsoDep.class.getName()}, {NfcA.class.getName()},
                {MifareClassic.class.getName()}, {MifareUltralight.class.getName()},
                {Ndef.class.getName()}
            };
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!readMode) return;
        String action = intent.getAction();
        if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) &&
            !NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) return;

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) return;

        lastTagUid = bytesToHex(tag.getId());
        StringBuilder info = new StringBuilder("UID: " + lastTagUid + "\nTechs: ");
        for (String t : tag.getTechList()) info.append(t.substring(t.lastIndexOf('.') + 1)).append(" ");

        // Try to read raw data from various tech types
        byte[] data = tryReadIsoDep(tag);
        if (data == null) data = tryReadMifare(tag);
        if (data == null) data = tryReadNdef(tag);
        if (data == null) data = tag.getId(); // fallback: just use UID

        lastTagData = data;
        info.append("\nData: ").append(bytesToHex(Arrays.copyOf(data, Math.min(data.length, 16)))).append("...");
        info.append("\n✓ Card read! Press SAVE to store.");
        tvStatus.setText(info.toString());
        btnSave.setEnabled(true);
        readMode = false;
    }

    private byte[] tryReadIsoDep(Tag tag) {
        try {
            IsoDep iso = IsoDep.get(tag);
            if (iso == null) return null;
            iso.connect();
            // Select PPSE
            byte[] resp = iso.transceive(new byte[]{
                0x00, (byte)0xA4, 0x04, 0x00, 0x0E,
                0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59,
                0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31, 0x00
            });
            iso.close();
            return resp;
        } catch (Exception e) { return null; }
    }

    private byte[] tryReadMifare(Tag tag) {
        try {
            MifareClassic mc = MifareClassic.get(tag);
            if (mc == null) return null;
            mc.connect();
            mc.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
            byte[] block = mc.readBlock(0);
            mc.close();
            return block;
        } catch (Exception e) {
            try {
                MifareUltralight mu = MifareUltralight.get(tag);
                if (mu == null) return null;
                mu.connect();
                byte[] data = mu.readPages(0);
                mu.close();
                return data;
            } catch (Exception e2) { return null; }
        }
    }

    private byte[] tryReadNdef(Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) return null;
            ndef.connect();
            byte[] data = ndef.getNdefMessage().toByteArray();
            ndef.close();
            return data;
        } catch (Exception e) { return null; }
    }

    static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i+1), 16));
        return data;
    }
}
