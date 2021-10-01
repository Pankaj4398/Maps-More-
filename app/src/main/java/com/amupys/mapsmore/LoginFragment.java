package com.amupys.mapsmore;

import android.content.Context;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends BottomSheetDialogFragment {

    private EditText et_email, et_pass;
    private Button bt_submit;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    Context context;

    public LoginFragment(Context context, FirebaseUser user, FirebaseAuth firebaseAuth){
        this.context = context;
        this.user = user;
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        et_email = v.findViewById(R.id.login_name);
        et_pass = v.findViewById(R.id.login_password);
        bt_submit = v.findViewById(R.id.btn_login);

        bt_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        if(user != null){
            AddFragment addFragment = new AddFragment(context);
            addFragment.show(getActivity().getSupportFragmentManager(), "bottomMenu");
            dismiss();
        }

        return v;
    }

    private void signIn() {
        if(et_email.getText().toString().isEmpty() || et_pass.getText().toString().isEmpty()){
            et_email.setError("Required*");
            et_pass.setError("Required*");
        }else {
            et_email.setError(null);
            et_pass.setError(null);

            firebaseAuth.createUserWithEmailAndPassword(et_email.getText().toString(), et_pass.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(context, "Successfully registered", Toast.LENGTH_SHORT).show();
                        AddFragment addFragment = new AddFragment(context);
                        addFragment.show(getActivity().getSupportFragmentManager(), "bottomMenu");
                        MainActivity.getInstance().btn_logout.setVisibility(View.VISIBLE);
                        dismiss();
                    }else {
                        if(task.getException().getMessage().contains("The email address is already in use by another account.")){
                            firebaseAuth.signInWithEmailAndPassword(et_email.getText().toString(), et_pass.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(context, "Logged in successfully", Toast.LENGTH_SHORT).show();
                                        AddFragment addFragment = new AddFragment(context);
                                        addFragment.show(getActivity().getSupportFragmentManager(), "bottomMenu");
                                        MainActivity.getInstance().btn_logout.setVisibility(View.VISIBLE);
                                        dismiss();
                                    }else {
                                        Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }else
                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}