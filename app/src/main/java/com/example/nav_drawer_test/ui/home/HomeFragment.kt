package com.example.nav_drawer_test.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.nav_drawer_test.BuildConfig
import com.example.nav_drawer_test.MainActivity
import com.example.nav_drawer_test.R
import com.example.nav_drawer_test.databinding.FragmentHomeBinding
import com.example.nav_drawer_test.ui.SharedViewModel
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.support.v4.runOnUiThread
import timber.log.Timber





// https://stackoverflow.com/questions/54581071/fragments-destroyed-recreated-with-jetpacks-android-navigation-components
class HomeFragment : Fragment() {

//    private lateinit var homeViewModel: HomeViewModel
    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.mobile_navigation)// activityViewModels()
//    private lateinit var sharedViewModel: SharedViewModel
//    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
//    private val binding get() = _binding!!

    private lateinit var binding: FragmentHomeBinding

//    var scanButton: Button? = null
    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
//        homeViewModel =
//                ViewModelProvider(this).get(HomeViewModel::class.java)
//        sharedViewModel =
//            ViewModelProvider(this).get(SharedViewModel::class.java)

//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        if (this::binding.isInitialized) {
            binding
            (binding.root.parent as ViewGroup?)?.removeView(binding.root)
        } else {
            binding = FragmentHomeBinding.inflate(inflater, container, false)
        }
        val root: View = binding.root

        Timber.i("binding.root = $root")  // Change upon switching back
        Timber.i("sharedViewModel = $sharedViewModel")    // Change upon switching back

        mainActivity.getNavBackStackEntryCount()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = binding.textHome
        sharedViewModel.text_home.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val scanButton: Button = binding.scanButton
//        scanButton.setOnClickListener {
////            sharedViewModel.onClickHomeButton(getString(R.string.text_home))
//            mainActivity.onClickScanButton()
//        }
//        Timber.i("scanButton = $scanButton")

//        runOnUiThread {
//            scan_button.text = if (mainActivity.isScanning()) "Stop Scan" else "Start Scan"
//        }

        val recyclerView: RecyclerView = binding.scanResultsRecyclerView
//        Timber.i("recyclerView = $recyclerView")

        mainActivity.onHomeFragmentViewCreated(recyclerView, scanButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
        mainActivity.onHomeFragmentDestroyView()
    }

}