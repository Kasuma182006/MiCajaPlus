package com.example.micaja

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView

fun setupKeyboardBehavior(
    rootView: View,
    viewToScroll: View,
    viewToHide: View? = null
) {
    val scrollView = viewToScroll as? NestedScrollView

    ViewCompat.setWindowInsetsAnimationCallback(
        rootView,
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val extraPadding = (imeBottom - navBottom).coerceAtLeast(0)

                // Ajusta el padding inferior del scroll para que el teclado no tape nada
                viewToScroll.setPadding(
                    viewToScroll.paddingLeft,
                    viewToScroll.paddingTop,
                    viewToScroll.paddingRight,
                    extraPadding
                )

                // Hace scroll automático al input enfocado
                if (extraPadding > 0) {
                    scrollView?.let { sv ->
                        val focused = sv.findFocus()
                        focused?.let { focusedView ->
                            sv.post {
                                val scrollBounds = android.graphics.Rect()
                                sv.getHitRect(scrollBounds)

                                val focusedRect = android.graphics.Rect()
                                focusedView.getDrawingRect(focusedRect)
                                sv.offsetDescendantRectToMyCoords(focusedView, focusedRect)

                                // Si el input queda debajo del área visible, hace scroll
                                val visibleBottom = sv.scrollY + sv.height - extraPadding
                                if (focusedRect.bottom > visibleBottom) {
                                    val scrollTo = focusedRect.bottom - sv.height + extraPadding + 32
                                    sv.smoothScrollTo(0, scrollTo)
                                }
                            }
                        }
                    }
                }

                // Ocultar logo suavemente si existe
                viewToHide?.let {
                    it.alpha = if (extraPadding > 200) 0f else 1f - (extraPadding / 200f)
                    it.visibility = if (it.alpha == 0f) View.GONE else View.VISIBLE
                }

                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                val imeVisible = ViewCompat.getRootWindowInsets(rootView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true

                if (!imeVisible) {
                    viewToScroll.setPadding(
                        viewToScroll.paddingLeft,
                        viewToScroll.paddingTop,
                        viewToScroll.paddingRight,
                        0
                    )
                    viewToHide?.alpha = 1f
                    viewToHide?.visibility = View.VISIBLE
                } else {
                    // Teclado ya abierto y se cambió de input — scroll al nuevo enfocado
                    scrollView?.let { sv ->
                        val focused = sv.findFocus()
                        focused?.let { focusedView ->
                            sv.post {
                                val imeBottom = ViewCompat.getRootWindowInsets(rootView)
                                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                                val navBottom = ViewCompat.getRootWindowInsets(rootView)
                                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                                val padding = (imeBottom - navBottom).coerceAtLeast(0)

                                val focusedRect = android.graphics.Rect()
                                focusedView.getDrawingRect(focusedRect)
                                sv.offsetDescendantRectToMyCoords(focusedView, focusedRect)

                                val visibleBottom = sv.scrollY + sv.height - padding
                                if (focusedRect.bottom > visibleBottom) {
                                    val scrollTo = focusedRect.bottom - sv.height + padding + 32
                                    sv.smoothScrollTo(0, scrollTo)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // Listener de foco: cuando el usuario toca un input distinto
    // con el teclado ya abierto, hace scroll automáticamente
    scrollView?.let { sv ->
        sv.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus == null) return@addOnGlobalFocusChangeListener

            val imeVisible = ViewCompat.getRootWindowInsets(rootView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true

            if (imeVisible) {
                sv.post {
                    val imeBottom = ViewCompat.getRootWindowInsets(rootView)
                        ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                    val navBottom = ViewCompat.getRootWindowInsets(rootView)
                        ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                    val padding = (imeBottom - navBottom).coerceAtLeast(0)

                    val focusedRect = android.graphics.Rect()
                    newFocus.getDrawingRect(focusedRect)
                    sv.offsetDescendantRectToMyCoords(newFocus, focusedRect)

                    val visibleBottom = sv.scrollY + sv.height - padding
                    if (focusedRect.bottom > visibleBottom) {
                        val scrollTo = focusedRect.bottom - sv.height + padding + 32
                        sv.smoothScrollTo(0, scrollTo)
                    }
                }
            }
        }
    }
}