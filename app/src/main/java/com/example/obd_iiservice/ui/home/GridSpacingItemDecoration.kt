package com.example.obd_iiservice.ui.home

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int, // Spacing dalam piksel
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view) // Posisi item
        val column = position % spanCount // Kolom item (0, 1, 2, ...)


        if (includeEdge) {
            // Jika ingin ada jarak juga di tepi luar grid
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) { // Baris paling atas
                outRect.top = spacing
            }
            outRect.bottom = spacing // Jarak bawah untuk setiap item
        } else {
            // Hanya jarak di antara item, tanpa jarak di tepi luar
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing // Jarak atas untuk semua baris kecuali yang pertama
            }
        }
    }
}

// Fungsi bantuan untuk konversi dp ke piksel
fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}