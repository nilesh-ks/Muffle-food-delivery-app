package com.project.muffle.fragment


import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.project.muffle.R
import com.project.muffle.util.DrawerLocker

/**
 * A simple [Fragment] subclass.
 */
class ProfileFragment : Fragment() {

    private lateinit var txtUserName: TextView
    private lateinit var txtPhone: TextView
    private lateinit var txtAddress: TextView
    private lateinit var txtEmail: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var imgUserImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        (activity as DrawerLocker).setDrawerEnabled(true)
        sharedPrefs = (activity as FragmentActivity).getSharedPreferences("FoodApp", Context.MODE_PRIVATE)
        txtUserName = view.findViewById(R.id.txtUserName)
        txtPhone = view.findViewById(R.id.txtPhone)
        txtEmail = view.findViewById(R.id.txtEmail)
        txtAddress = view.findViewById(R.id.txtAddress)
        imgUserImage=view.findViewById(R.id.imgUserImage) as ImageView
        txtUserName.text = sharedPrefs.getString("user_name", null)
        val phoneText = "+91-${sharedPrefs.getString("user_mobile_number", null)}"
        txtPhone.text = phoneText
        txtEmail.text = sharedPrefs.getString("user_email", null)
        val address = sharedPrefs.getString("user_address", null)
        txtAddress.text = address


        imgUserImage.setOnClickListener{
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                //if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED){
                    //Permission denied
                    val permissions=arrayOf(READ_EXTERNAL_STORAGE)
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE)


                }else{
                    //permission already granted
                    pickImageFromGallery();
                }
            }/*else{
                //system OS is < Marshmallow
                pickImageFromGallery();
            }
        }*/
        return view
    }
    private fun pickImageFromGallery(){
        val intent= Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)

    }
    companion object{
        //image pick code
        private val IMAGE_PICK_CODE=1000;
        //permission code
        private val PERMISSION_CODE=1001;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode)
        {
            PERMISSION_CODE->{
                if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }else{
                    //permission from popup denied
                        Toast.makeText(activity as Context,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK && requestCode== IMAGE_PICK_CODE)
        {
            imgUserImage.setImageURI(data?.data)
        }
    }

}
