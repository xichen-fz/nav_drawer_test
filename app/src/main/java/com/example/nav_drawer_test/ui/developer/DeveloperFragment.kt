package com.example.nav_drawer_test.ui.developer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.nav_drawer_test.MainActivity
import com.example.nav_drawer_test.R
import com.example.nav_drawer_test.databinding.FragmentDeveloperBinding
import com.example.nav_drawer_test.databinding.FragmentHomeBinding
import com.example.nav_drawer_test.ui.SharedViewModel
import timber.log.Timber

class DeveloperFragment : Fragment() {

//    private lateinit var developerViewModel: DeveloperViewModel
    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.mobile_navigation)// activityViewModels()
//    private var _binding: FragmentDeveloperBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
//    private val binding get() = _binding!!

    private lateinit var binding: FragmentDeveloperBinding

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
//        developerViewModel =
//                ViewModelProvider(this).get(DeveloperViewModel::class.java)

//        _binding = FragmentDeveloperBinding.inflate(inflater, container, false)
        if (this::binding.isInitialized) {
            binding
            (binding.root.parent as ViewGroup?)?.removeView(binding.root)
        } else {
            binding = FragmentDeveloperBinding.inflate(inflater, container, false)
        }
        val root: View = binding.root

        Timber.i("binding.root = $root")  // Change upon switching back
        Timber.i("sharedViewModel = $sharedViewModel")

        mainActivity.getNavBackStackEntryCount()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = binding.textDeveloper
        sharedViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val requestMtuButton: Button = binding.requestMtuButton

        val recyclerView: RecyclerView = binding.characteristicsRecyclerView
        Timber.i("recyclerView = $recyclerView")

        val logTextView: TextView = binding.logTextView
        val logScrollView: ScrollView = binding.logScrollView

        mainActivity.onDeveloperFragmentViewCreated(
            recyclerView, requestMtuButton, logTextView, logScrollView
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
        mainActivity.onDeveloperFragmentDestroyView()
    }
}