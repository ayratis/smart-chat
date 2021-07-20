// Generated by view binder compiler. Do not edit!
package gb.smartchat.library.ui._global.viewbinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import gb.smartchat.R;

public final class FragmentUsernameMissingBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final Button btnContinue;

  @NonNull
  public final TextInputLayout tilName;

  @NonNull
  public final TextInputEditText etName;

  @NonNull
  public final Toolbar toolbar;

  @NonNull
  public final TextView tvMessage;

  @NonNull
  public final ScrollView scrollView;

  private FragmentUsernameMissingBinding(@NonNull ConstraintLayout rootView,
      @NonNull Button btnContinue, @NonNull TextInputLayout tilName,
      @NonNull TextInputEditText etName, @NonNull Toolbar toolbar, @NonNull TextView tvMessage,
      @NonNull ScrollView scrollView) {
    this.rootView = rootView;
    this.btnContinue = btnContinue;
    this.tilName = tilName;
    this.etName = etName;
    this.toolbar = toolbar;
    this.tvMessage = tvMessage;
    this.scrollView = scrollView;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentUsernameMissingBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentUsernameMissingBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_username_missing, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentUsernameMissingBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btn_continue;
      Button btnContinue = rootView.findViewById(id);
      if (btnContinue == null) {
        break missingId;
      }

      id = R.id.til_name;
      TextInputLayout tilName = rootView.findViewById(id);
      if (tilName == null) {
        break missingId;
      }

      id = R.id.et_name;
      TextInputEditText etName = rootView.findViewById(id);
      if (etName == null) {
        break missingId;
      }

      id = R.id.toolbar;
      Toolbar toolbar = rootView.findViewById(id);
      if (toolbar == null) {
        break missingId;
      }

      id = R.id.tv_message;
      TextView tvMessage = rootView.findViewById(id);
      if (tvMessage == null) {
        break missingId;
      }

      id = R.id.scroll_view;
      ScrollView scrollView = rootView.findViewById(id);
      if (scrollView == null) {
        break missingId;
      }

      return new FragmentUsernameMissingBinding((ConstraintLayout) rootView, btnContinue, tilName,
          etName, toolbar, tvMessage, scrollView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}