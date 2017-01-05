package gliderkite.pswcode;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a single user credential.
 */
final class Credential implements Parcelable
{
    /**
     * Constructor.
     * @param service The name of the service.
     * @param username The user name.
     * @param password The password.
     */
    public Credential(String service, String username, String password)
    {
        Service = service;
        Username = username;
        Password = password;
    }

    public String Service;
    public String Username;
    public String Password;


    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(Service);
        dest.writeString(Username);
        dest.writeString(Password);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Parcelable.Creator<Credential> CREATOR = new Parcelable.Creator<Credential>()
    {
        @Override
        public Credential createFromParcel(Parcel in)
        {
            return new Credential(in);
        }

        @Override
        public Credential[] newArray(int size)
        {
            return new Credential[size];
        }
    };


    public Credential(Parcel in)
    {
        Service = in.readString();
        Username = in.readString();
        Password = in.readString();
    }


}
