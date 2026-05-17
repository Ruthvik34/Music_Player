package com.ruthvik.musicplayer.BottomSheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ruthvik.musicplayer.R
import com.ruthvik.musicplayer.databinding.BottomSheetCreatePlaylistBinding

class MusicPlayerCreatePlaylistBottomSheet(
    mContext: Context,
    private val listener: OnOptionClick?
) : BottomSheetDialog(
    mContext,
    R.style.BottomSheetDialogTheme
) {

    private lateinit var binding: BottomSheetCreatePlaylistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BottomSheetCreatePlaylistBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        setListener()
        setupBottomSheetStyle()
    }

    private fun setListener() {
        binding.ivCloseBtn.setOnClickListener {
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            val playlistName = binding.etPlaylistName.text.toString().trim()
            if (playlistName.isNotEmpty()) {
                listener?.onOptionSelect(playlistName)
                dismiss()
            } else {
                binding.etPlaylistName.error = context.getString(R.string.playlist_name_required)
            }
        }
    }

    private fun setupBottomSheetStyle() {
        val bottomSheet = findViewById<ViewGroup>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.setBackgroundResource(R.color.surface_card)
    }

    interface OnOptionClick {
        fun onOptionSelect(playlistName: String)
    }
}
