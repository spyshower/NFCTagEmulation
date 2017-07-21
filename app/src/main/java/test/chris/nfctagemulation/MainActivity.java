package test.chris.nfctagemulation;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
	
	// Mock Tag object variables
	
	final int TECH_NFC_A = 1;
	final String EXTRA_NFC_A_SAK = "sak";    // short (SAK byte value)
	final String EXTRA_NFC_A_ATQA = "atqa";  // byte[2] (ATQA value)
	
	final int TECH_NDEF = 6;
	final String EXTRA_NDEF_MSG = "ndefmsg";              // NdefMessage (Parcelable)
	final String EXTRA_NDEF_MAXLENGTH = "ndefmaxlength";  // int (result for getMaxSize())
	final String EXTRA_NDEF_CARDSTATE = "ndefcardstate";  // int (1: read-only, 2: read/write, 3: unknown)
	final String EXTRA_NDEF_TYPE = "ndeftype";            // int (1: T1T, 2: T2T, 3: T3T, 4: T4T, 101: MF Classic, 102: ICODE)
	
	
	String message = "-";
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		final Handler h = new Handler();
		
		h.postDelayed(new Runnable(){
			public void run() {
				Log.d("post delayed", "message is: " + message);
				if (message.equals("unlock")) {
					message = "-";
				}
				else {
					if (message.equals("-")) {
						message = "unlock";
					}
				}
				NdefMessage ndefMsg = createNdfMessage( );
				
				Intent ndefIntent = createIntent( ndefMsg );
				startActivity( ndefIntent );
				finish();
				h.postDelayed(this, 7000);
			}
		}, 7000);
		
		
	}
	
	Intent createIntent (NdefMessage ndefMsg) {
		
		Class tagClass = Tag.class;
		Method createMockTagMethod = null;
		try {
			createMockTagMethod = tagClass.getMethod("createMockTag", byte[].class, int[].class, Bundle[].class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace( );
		}
		
		
		Bundle nfcaBundle = new Bundle();
		nfcaBundle.putByteArray(EXTRA_NFC_A_ATQA, new byte[]{ (byte)0x44, (byte)0x00 }); //ATQA for Type 2 tag
		nfcaBundle.putShort(EXTRA_NFC_A_SAK , (short)0x00); //SAK for Type 2 tag
		
		Bundle ndefBundle = new Bundle();
		ndefBundle.putInt(EXTRA_NDEF_MAXLENGTH, 48); // maximum message length: 48 bytes
		ndefBundle.putInt(EXTRA_NDEF_CARDSTATE, 1); // read-only
		ndefBundle.putInt(EXTRA_NDEF_TYPE, 2); // Type 2 tag
		ndefBundle.putParcelable(EXTRA_NDEF_MSG, ndefMsg);  // add an NDEF message
		
		byte[] tagId = new byte[] { (byte)0x3F, (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78, (byte)0x90, (byte)0xAB };
		
		Tag mockTag = null;
		try {
			mockTag = (Tag)createMockTagMethod.invoke(null,
					tagId,                                     // tag UID/anti-collision identifier (see Tag.getId() method)
					new int[] { TECH_NFC_A, TECH_NDEF },       // tech-list
					new Bundle[] { nfcaBundle, ndefBundle });  // array of tech-extra bundles, each entry maps to an entry in the tech-list
		} catch (IllegalAccessException e) {
			e.printStackTrace( );
		} catch (InvocationTargetException e) {
			e.printStackTrace( );
		}
		
		Intent ndefIntent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED);
		ndefIntent.setType("text/plain");
		ndefIntent.putExtra(NfcAdapter.EXTRA_ID, tagId);
		ndefIntent.putExtra(NfcAdapter.EXTRA_TAG, mockTag);
		ndefIntent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[]{ ndefMsg });
		
		return ndefIntent;
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	NdefMessage createNdfMessage() {
		
		NdefRecord record1 = null;
		try {
			record1 = NdefRecord.createMime("text/plain", "Text".getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace( );
		}
		NdefRecord record2 = NdefRecord.createTextRecord("en", message);
		return new NdefMessage(new NdefRecord[] {record1, record2});
		
	}
	
}
