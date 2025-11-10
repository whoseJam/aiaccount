package com.aiaccounting.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aiaccounting.app.databinding.ActivityMainBinding
import com.aiaccounting.app.ui.ChatFragment
import com.aiaccounting.app.ui.SettingsFragment
import com.aiaccounting.app.ui.StatisticsFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    // Tab icons
    private val tabIcons = arrayOf(
        R.drawable.ic_tab_accounting,
        R.drawable.ic_tab_statistics,
        R.drawable.ic_tab_settings
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
    }
    
    private fun setupViewPager() {
        // Create ViewPager adapter
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Enable smooth scrolling with optimized settings
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.offscreenPageLimit = 1  // Pre-load adjacent pages
        
        // Add smooth page transformer
        binding.viewPager.setPageTransformer { page, position ->
            page.apply {
                val absPosition = Math.abs(position)
                alpha = 1f - (absPosition * 0.3f).coerceIn(0f, 1f)
                val scale = 1f - (absPosition * 0.05f).coerceIn(0f, 0.05f)
                scaleX = scale
                scaleY = scale
            }
        }
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()
    }
    
    /**
     * ViewPager2 Adapter for managing fragments
     */
    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChatFragment()
                1 -> StatisticsFragment()
                2 -> SettingsFragment()
                else -> ChatFragment()
            }
        }
    }
}
