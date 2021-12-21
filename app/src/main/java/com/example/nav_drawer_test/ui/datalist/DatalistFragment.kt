package com.example.nav_drawer_test.ui.datalist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import com.example.nav_drawer_test.MainActivity
import com.example.nav_drawer_test.R
import com.example.nav_drawer_test.databinding.FragmentDatalistBinding
import com.example.nav_drawer_test.ui.SharedViewModel

class DatalistFragment : Fragment() {

//    private lateinit var datalistViewModel: DatalistViewModel
    private val sharedVewModel: SharedViewModel by navGraphViewModels(R.id.mobile_navigation)// activityViewModels()
    private var _binding: FragmentDatalistBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
//        datalistViewModel =
//                ViewModelProvider(this).get(DatalistViewModel::class.java)

        _binding = FragmentDatalistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDatalist
        sharedVewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = getString(R.string.text_data) // it
        })

        mainActivity.getNavBackStackEntryCount()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}