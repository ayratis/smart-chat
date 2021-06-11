package gb.smartchat.library.utils

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import gb.smartchat.R
import kotlin.reflect.KClass


const val defaultContainerId = R.id.fragment_container

fun FragmentManager.back() {
    popBackStack()
}

fun FragmentManager.backTo(fragmentClass: KClass<out Fragment>?) {
    if (fragmentClass == null) {
        popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    } else {
        popBackStack(fragmentClass.java.canonicalName, 0)
    }
    executePendingTransactions()
}

fun FragmentManager.navigateTo(
    fragment: Fragment,
    navAnim: NavAnim = NavAnim.NONE,
    setupFragmentTransaction: ((FragmentTransaction) -> Unit)? = null,
    containerId: Int = defaultContainerId
) {
    val fragmentTransaction = beginTransaction()
    setupFragmentTransaction?.invoke(fragmentTransaction)
    when (navAnim) {
        NavAnim.SLIDE -> fragmentTransaction.setSlideAnimation()
        NavAnim.OPEN -> fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        NavAnim.NONE -> {
        }
    }
    fragmentTransaction
        .replace(containerId, fragment)
        .addToBackStack(fragment.javaClass.canonicalName)
        .setReorderingAllowed(true)
        .commit()
    executePendingTransactions()
}

fun FragmentManager.replace(
    fragment: Fragment,
    navAnim: NavAnim = NavAnim.NONE,
    setupFragmentTransaction: ((FragmentTransaction) -> Unit)? = null,
    containerId: Int = defaultContainerId
) {
    if (backStackEntryCount > 0) {
        popBackStack()
        val fragmentTransaction = beginTransaction()
        setupFragmentTransaction?.invoke(fragmentTransaction)
        when (navAnim) {
            NavAnim.SLIDE -> fragmentTransaction.setSlideAnimation()
            NavAnim.OPEN -> fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            NavAnim.NONE -> {
            }
        }
        fragmentTransaction
            .replace(containerId, fragment)
            .addToBackStack(fragment.javaClass.canonicalName)
            .setReorderingAllowed(true)
            .commit()
    } else {
        val fragmentTransaction = beginTransaction()
        setupFragmentTransaction?.invoke(fragmentTransaction)
        when (navAnim) {
            NavAnim.SLIDE -> fragmentTransaction.setSlideAnimation()
            NavAnim.OPEN -> fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            NavAnim.NONE -> {
            }
        }
        fragmentTransaction
            .replace(containerId, fragment)
            .setReorderingAllowed(true)
            .commit()
    }
    executePendingTransactions()
}

fun FragmentManager.newRootScreen(
    fragment: Fragment,
    navAnim: NavAnim = NavAnim.NONE,
    setupFragmentTransaction: ((FragmentTransaction) -> Unit)? = null,
    containerId: Int = defaultContainerId,
) {
    backTo(null)
    replace(fragment, navAnim, setupFragmentTransaction, containerId)
}

fun FragmentManager.newScreenFromRoot(
    fragment: Fragment,
    navAnim: NavAnim = NavAnim.NONE,
    setupFragmentTransaction: ((FragmentTransaction) -> Unit)? = null,
    containerId: Int = defaultContainerId,
) {
    popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    replace(fragment, navAnim, setupFragmentTransaction, containerId)
}

fun FragmentManager.newRootChain(
    vararg fragments: Fragment,
    navAnim: NavAnim = NavAnim.NONE,
    setupFragmentTransaction: ((FragmentTransaction) -> Unit)? = null,
    containerId: Int = defaultContainerId
) {
    backTo(null)
    if (fragments.isNotEmpty()) {
        replace(
            fragments[0],
            navAnim,
            setupFragmentTransaction,
            containerId
        )
        for (i in 1 until fragments.size) {
            navigateTo(
                fragments[i],
                navAnim,
                setupFragmentTransaction,
                containerId
            )
        }
    }
}

fun Fragment.registerOnBackPress(
    onBackPressed: () -> Unit
) {
    requireActivity().onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed.invoke()
            }
        })
}

fun FragmentTransaction.setSlideAnimation(): FragmentTransaction {
    return setCustomAnimations(
        R.anim.slide_in_right,
        R.anim.slide_out_left,
        R.anim.slide_in_left,
        R.anim.slide_out_right
    )
}

enum class NavAnim {
    SLIDE,
    OPEN,
    NONE
}
