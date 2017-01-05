package gliderkite.pswcode;

import android.app.Activity;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity
{

    /**
     * Username EditText.
     */
    private EditText usernameText;

    /**
     * Password EditText.
     */
    private EditText passwordText;

    /**
     * List of credentials read from the database.
     */
    private List<Credential> credentials;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // initialize username and password EditTexts
        usernameText = (EditText)findViewById(R.id.UsernameET);
        assert usernameText != null;
        passwordText = (EditText)findViewById(R.id.PasswordET);
        assert passwordText != null;
        // initialize login button behavior
        Button loginButton = (Button)findViewById(R.id.LoginButton);
        assert loginButton != null;
        // set listener
        loginButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onLogin();
            }
        });
        // initialize register button behavior
        Button registerButton = (Button)findViewById(R.id.RegisterButton);
        assert registerButton != null;
        // set listener
        registerButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onRegister();
            }
        });
    }


    /**
     * Attempts login.
     */
    private void onLogin()
    {
        assert usernameText != null;
        assert passwordText != null;

        try
        {
            usernameText.setError(null);
            passwordText.setError(null);

            if (isTextFieldValid(usernameText) && isTextFieldValid(passwordText))
            {
                String username = usernameText.getText().toString();
                String masterkey = passwordText.getText().toString();
                String filename = Database.getFilename(username, masterkey);

                credentials = Database.read(getApplicationContext(), filename, masterkey);

                if (credentials != null && !credentials.isEmpty())
                {
                    Intent myIntent = new Intent(LoginActivity.this, PswActivity.class);
                    myIntent.putExtra("filename", filename);
                    myIntent.putExtra("key", masterkey);
                    myIntent.putParcelableArrayListExtra("db", (ArrayList<? extends Parcelable>) credentials);
                    LoginActivity.this.startActivity(myIntent);
                }

                usernameText.setText(null);
                passwordText.setText(null);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * Attempts registration.
     */
    private void onRegister()
    {
        assert usernameText != null;
        assert passwordText != null;

        try
        {
            usernameText.setError(null);
            passwordText.setError(null);

            if (isTextFieldValid(usernameText) && isTextFieldValid(passwordText))
            {
                String username = usernameText.getText().toString();
                String masterkey = passwordText.getText().toString();
                String filename = Database.getFilename(username, masterkey);

                // create an empty database
                credentials = new ArrayList<>();
                boolean res = Database.write(getApplicationContext(), filename, credentials, masterkey);

                if (res)
                {
                    Intent myIntent = new Intent(LoginActivity.this, PswActivity.class);
                    myIntent.putExtra("filename", filename);
                    myIntent.putExtra("key", masterkey);
                    myIntent.putParcelableArrayListExtra("db", (ArrayList<? extends Parcelable>) credentials);
                    LoginActivity.this.startActivity(myIntent);
                }

                usernameText.setText(null);
                passwordText.setText(null);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * Returns true only if the text could be valid.
     * @param field EditText to check.
     * @return
     */
    private static boolean isTextFieldValid(EditText field)
    {
        if (field == null)
            return false;

        String text = field.getText().toString();

        // check text
        if (text.isEmpty())
        {
            field.setError("This field can not be empty");
            return false;
        }

        return true;
    }




}
