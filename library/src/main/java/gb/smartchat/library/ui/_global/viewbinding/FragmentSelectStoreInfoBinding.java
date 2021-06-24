// Generated by view binder compiler. Do not edit!
package gb.smartchat.library.ui._global.viewbinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import gb.smartchat.R;

public final class FragmentSelectStoreInfoBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final RecyclerView rvStoreInfo;

  @NonNull
  public final Toolbar toolbar;

  private FragmentSelectStoreInfoBinding(@NonNull LinearLayout rootView,
      @NonNull RecyclerView rvStoreInfo, @NonNull Toolbar toolbar) {
    this.rootView = rootView;
    this.rvStoreInfo = rvStoreInfo;
    this.toolbar = toolbar;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentSelectStoreInfoBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentSelectStoreInfoBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_select_store_info, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentSelectStoreInfoBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.rv_store_info;
      RecyclerView rvStoreInfo = rootView.findViewById(id);
      if (rvStoreInfo == null) {
        break missingId;
      }

      id = R.id.toolbar;
      Toolbar toolbar = rootView.findViewById(id);
      if (toolbar == null) {
        break missingId;
      }

      return new FragmentSelectStoreInfoBinding((LinearLayout) rootView, rvStoreInfo, toolbar);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}