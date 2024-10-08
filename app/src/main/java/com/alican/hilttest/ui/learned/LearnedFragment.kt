package com.alican.hilttest.ui.learned

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alican.hilttest.databinding.FragmentLearnedBinding
import com.app.wordy.data.Word
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@AndroidEntryPoint
class LearnedFragment : Fragment() {
    private var _binding: FragmentLearnedBinding? = null
    private val binding get() = _binding!!

    private lateinit var learnedAdapter: LearnedWordsAdapter
    private val viewModel by viewModels<LearnedViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLearnedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        initCollectors()
        val words = loadWordsFromJson()
        viewModel.updateLearnedWords(words)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
        }

    }
    private fun initAdapter() {
        learnedAdapter = LearnedWordsAdapter( callback = { word: Word ->
            showWordPopup(word = word)
        })
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = learnedAdapter
        }
    }
    private fun initCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.words.collectLatest { words ->
                val wordsList = words.map {
                    it.copy(isLearned = viewModel.isSavedLearned(it))
                }.filter { it.isLearned }
                learnedAdapter.submitList(wordsList) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun showWordPopup(word: Word) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Remove Word")
            .setMessage("Do you want to remove '${word.word}' from learned?")
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.removeLearnedWord(word)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun loadWordsFromJson(): List<Word> {
        val inputStream = requireActivity().assets.open("words.json")
        val reader = InputStreamReader(inputStream)
        val wordType = object : TypeToken<List<Word>>() {}.type
        return Gson().fromJson(reader, wordType)
    }
}