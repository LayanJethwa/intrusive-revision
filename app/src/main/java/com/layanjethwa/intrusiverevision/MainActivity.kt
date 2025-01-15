package com.layanjethwa.intrusiverevision

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.layanjethwa.intrusiverevision.databinding.LayoutBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: LayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        val fragments = listOf(
            FlashcardsFragment(),
            HomeFragment(),
            SettingsFragment()
        )

        val adapter = MyFragmentAdapter(
            fragments,
            supportFragmentManager,
            lifecycle
        )

        binding.pager.adapter = adapter
        binding.pager.post {
            binding.pager.setCurrentItem(1,true)
        }

        TabLayoutMediator(binding.tabLayout, binding.pager){tab, position ->
            tab.text = when(position){
                0 -> "Flashcards"
                1 -> "Questions"
                else -> "Settings"
            }
        }.attach()


    }
}
