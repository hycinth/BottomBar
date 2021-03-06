package wenchieh.lu.bottombar

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Wenchieh.Lu  2018/5/9
 * 底栏
 *
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
class BottomBar @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  private var onSelectedListener: (prePosition: Int, position: Int, isManual: Boolean) -> Unit = { _, _, _ -> },
  private var onReSelectedListener: (position: Int) -> Unit = { }
) : LinearLayout(context, attrs, defStyleAttr) {

  private var mCurrentIndex = -1
  private var mPreviousIndex = -1
  private var isSetup = false
  private val tabList = arrayListOf<IBottomTab>()

  init {
    orientation = HORIZONTAL
  }

  /**
   * set a listener to trigger something when bottomTab is selected
   * @param prePosition the position of selected button previously
   * @param position the position of selected button just now
   */
  fun setOnSelectedListener(onSelectedListener: (prePosition: Int, position: Int, isManual: Boolean) -> Unit) {
    this.onSelectedListener = onSelectedListener
  }

  /**
   * set a listener to trigger something when bottomTab is reSelected
   * @param position the position of reSelected button just now
   */
  fun setOnReSelectedListener(
    onReSelectedListener: (position: Int) -> Unit) {
    this.onReSelectedListener = onReSelectedListener
  }

  private fun <T> applyView(view: T) where T : IBottomTab, T : View =
      view.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
          weight = 1f
        }
        if (id == View.NO_ID)
          id = if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            View.generateViewId()
          } else {
            generateViewIdV16()
          }
      }

  /**
   * init BottomTab
   */
  fun <T> addTab(vararg tabs: T) where T : IBottomTab, T : View {
    tabList.addAll(tabs)
    for (i in tabs.indices) {
      val tab = applyView(tabs[i])
      addViewInLayout(tab, -1, tab.layoutParams, true)
      tabs[i].setOnClickListener {
        selectItem(i,true)
      }
    }

    isSetup = true
    mCurrentIndex = 0
    currentTab().setSelected(true)
    requestLayout()
  }

  fun <T> addTab(tab: T) where T : IBottomTab, T : View {
    tabList.add(tab)
    val item = applyView(tab)
    item.setOnClickListener {
      selectItem(tabList.lastIndex,true)
    }
    addView(item)
  }

  override fun removeAllViews() {
    super.removeAllViews()
    tabList.clear()
  }

  /**
   * change positions and trigger listener
   * @param toIndex the next position to go
   * @param isManual true is select by user
   */
  fun selectItem(toIndex: Int,isManual: Boolean = false) {
    if (childCount == 0) return
    if (mCurrentIndex == toIndex && mPreviousIndex != -1) {
      if (isManual){
        onReSelectedListener(mCurrentIndex)
      }
      return
    }
    onSelected(mCurrentIndex, toIndex, isManual, onSelectedListener)
    mPreviousIndex = mCurrentIndex
    mCurrentIndex = toIndex
  }

  /**
   * @param preIndex
   * @param curIndex
   * @param isManual
   */
  private fun onSelected(
    preIndex: Int, curIndex: Int, isManual: Boolean, onSelectedListener: (prePosition: Int, position: Int, isManual: Boolean) -> Unit) {

    for (i in 0 until childCount) {
      getChildAt(i).isSelected = i == curIndex
    }

    onSelectedListener(preIndex, curIndex, isManual)
  }

  /**
   * save some state
   */
  override fun onSaveInstanceState(): Parcelable? {
    return SavedState(super.onSaveInstanceState() ?: return null).apply {
      preIndex = mPreviousIndex
      curIndex = mCurrentIndex
    }
  }

  /**
   * restore some state
   */
  override fun onRestoreInstanceState(state: Parcelable?) {
    if (state == null || state !is SavedState) {
      super.onRestoreInstanceState(state)
      return
    }
    super.onRestoreInstanceState(state.superState)
    restoreState(state)
  }

  private fun restoreState(savedState: SavedState) {
    mPreviousIndex = savedState.preIndex
    mCurrentIndex = savedState.curIndex
    post {
      onSelected(mPreviousIndex, mCurrentIndex, false, onSelectedListener)
    }
  }

  private class SavedState : BaseSavedState {
    var preIndex = -1
    var curIndex = 0


    constructor(superState: Parcelable) : super(superState)

    private constructor(parcel: Parcel) : super(parcel) {
      preIndex = parcel.readInt()
      curIndex = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
      super.writeToParcel(parcel, flags)
      parcel.writeInt(preIndex)
      parcel.writeInt(curIndex)
    }

    override fun describeContents(): Int {
      return 0
    }

    companion object CREATOR : Creator<SavedState> {
      override fun createFromParcel(parcel: Parcel): SavedState {
        return SavedState(parcel)
      }

      override fun newArray(size: Int): Array<SavedState?> {
        return arrayOfNulls(size)
      }
    }
  }

  /**
   * just select ui
   */
  fun setSelectState(index: Int) {
    post {
      mPreviousIndex = mCurrentIndex
      mCurrentIndex = index
      for (i in 0 until childCount) {
        getTab(i).setSelected(i == index)
      }
    }
  }


  private val sNextGeneratedId = AtomicInteger(1)
  private fun generateViewIdV16(): Int {
    while (true) {
      val result = sNextGeneratedId.get()
      // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
      var newValue = result + 1
      if (newValue > 0x00FFFFFF) newValue = 1 // Roll over to 1, not 0.
      if (sNextGeneratedId.compareAndSet(result, newValue)) {
        return result
      }
    }
  }

  /**
   * update child view
   * @param index the index of origin view which you want to update
   * @param view the new View instead of origin View
   */
  fun update(index: Int, view: BottomTab) {
    val originView = getChildAt(index) ?: return

    (originView as BottomTab).apply {
      iconNormal = view.iconNormal
      iconSelected = view.iconSelected
      iconNormalBt = view.iconNormalBt
      iconSelectedBt = view.iconSelectedBt
      text = view.text
      padding = view.padding
      textSize = view.textSize
      textColorNormal = view.textColorNormal
      textColorSelected = view.textColorSelected
      badgeBackgroundColor = view.badgeBackgroundColor
      badgeNumber = view.badgeNumber
      isShowPoint = view.isShowPoint
      updateTextBounds()
    }

    originView.requestLayout()
  }

  fun currentPosition() = mCurrentIndex
  fun prePosition() = mPreviousIndex

  fun currentTab() = getChildAt(mCurrentIndex) as IBottomTab

  fun setCurrentPosition(position: Int) {
    mCurrentIndex = position
  }

  fun getTab(position: Int) = getChildAt(position) as IBottomTab

  companion object {
    private const val TAG = "BottomBar"
  }
}
