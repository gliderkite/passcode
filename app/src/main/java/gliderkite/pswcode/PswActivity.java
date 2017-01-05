package gliderkite.pswcode;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;


public class PswActivity extends ListActivity
{
    /**
     * List view items.
     */
    private ArrayList<String> listItems = new ArrayList<String>();

    /**
     * List view string adapter
     */
    private ArrayAdapter<String> adapter;

    /**
     * Username EditText.
     */
    private EditText usernameText;

    /**
     * Password EditText.
     */
    private EditText passwordText;

    /**
     * Service EditText.
     */
    private EditText serviceText;

    /**
     * List of credentials.
     */
    private ArrayList<Credential> credentials;

    /**
     * Master key.
     */
    private String masterkey;

    /**
     * Database filename.
     */
    private String filename;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psw);

        // initialize service, username and password EditTexts
        serviceText = (EditText)findViewById(R.id.ServiceET);
        assert serviceText != null;
        usernameText = (EditText)findViewById(R.id.UsernameET);
        assert usernameText != null;
        passwordText = (EditText)findViewById(R.id.PasswordET);
        assert passwordText != null;
        // initialize login button behavior
        Button addButton = (Button)findViewById(R.id.AddButton);
        assert addButton != null;
        // set listener
        addButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onAddCredential();
            }
        });

        // create adapter
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        setListAdapter(adapter);

        // get credentials
        credentials = getIntent().getParcelableArrayListExtra("db");
        // get master key and database filename
        filename = getIntent().getStringExtra("filename");
        masterkey = getIntent().getStringExtra("key");

        if (credentials != null)
        {
            // insert credentials to the list view
            for (Credential c : credentials)
            {
                String content = String.format("\n%1$s\n%2$s\n%3$s", c.Service, c.Username, c.Password);
                adapter.add(content);
            }
        }
    }


    /**
     * Add a new credential.
     */
    private void onAddCredential()
    {
        assert serviceText != null;
        assert usernameText != null;
        assert passwordText != null;

        if (credentials == null)
            return;

        try
        {
            if (isTextFieldValid(serviceText) && isTextFieldValid(usernameText) && isTextFieldValid(passwordText))
            {
                String service = serviceText.getText().toString();
                String username = usernameText.getText().toString();
                String password = passwordText.getText().toString();

                credentials.add(new Credential(service, username, password));
                // update database
                boolean res = Database.write(getApplicationContext(), filename, credentials, masterkey);

                if (res)
                {
                    String content = String.format("\n%1$s\n%2$s\n%3$s", service, username, password);
                    adapter.insert(content, 0);
                }

                serviceText.setText(null);
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
