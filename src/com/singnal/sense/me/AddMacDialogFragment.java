package com.singnal.sense.me;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class AddMacDialogFragment extends DialogFragment{

	private EditText et_mac,et_func,et_content;
	private Button bt_ensure,bt_cancel;
	
	public AddMacDialogFragment(){
		
	}
	
	public interface AddMacDialogListener {
        void onFinishAddMacDialog(String inputMac,String inputFunc,String inputContent);
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add, container, false);
        getDialog().setTitle(R.string.add_dialog_title);
        
        et_mac = (EditText) view.findViewById(R.id.et_mac);
        et_func = (EditText) view.findViewById(R.id.et_func);
        et_content = (EditText) view.findViewById(R.id.et_content);
        bt_ensure = (Button) view.findViewById(R.id.bt_dialog_ensure);
        bt_ensure.setOnClickListener(new onClickListener());
        bt_cancel = (Button) view.findViewById(R.id.bt_dialog_cancel);
        bt_cancel.setOnClickListener(new onClickListener());
        
     // Show soft keyboard automatically
        et_mac.requestFocus();
        
        return view;
    }

	public class onClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.bt_dialog_cancel:
				AddMacDialogFragment.this.dismiss();
				et_mac.setText("");
				et_func.setText("");
				et_content.setText("");
				break;
			case R.id.bt_dialog_ensure:
				// Return input text to activity
				AddMacDialogListener activity = (AddMacDialogListener) getActivity();
		        activity.onFinishAddMacDialog(et_mac.getText().toString(),et_func.getText().toString(),et_content.getText().toString());
		        et_mac.setText("");
				et_func.setText("");
				et_content.setText("");
		        AddMacDialogFragment.this.dismiss();
		        break;
			}			
		}
	}
	
}
