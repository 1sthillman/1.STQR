package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Mini Takvim
 */
public class MiniCalendarView extends LinearLayout {
    private TextView monthYearText;
    private GridLayout calendarGrid;
    private Calendar currentCalendar;
    private Calendar selectedCalendar;
    private TextView selectedDateText;

    public interface CalendarCallback {
        void onDateSelected(String date);
        void onClose();
    }

    private final CalendarCallback callback;

    public MiniCalendarView(Context context, CalendarCallback callback) {
        super(context);
        this.callback = callback;
        try {
            this.currentCalendar = Calendar.getInstance();
            this.selectedCalendar = Calendar.getInstance();
        } catch (Exception e) {
            // Fallback to avoid NPE
            this.currentCalendar = Calendar.getInstance(new Locale("tr"));
            this.selectedCalendar = Calendar.getInstance(new Locale("tr"));
        }
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(12), dp(12), dp(12), dp(12));

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        
        TextView title = new TextView(context);
        title.setText("📅 Takvim");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        closeBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header);

        // Month/Year navigation
        LinearLayout navRow = new LinearLayout(context);
        navRow.setOrientation(HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        navRow.setPadding(0, 0, 0, dp(12));

        Button prevBtn = new Button(context);
        prevBtn.setText("◀");
        prevBtn.setTextSize(18);
        prevBtn.setTypeface(null, Typeface.BOLD);
        prevBtn.setTextColor(0xFFFFFFFF);
        prevBtn.setBackgroundColor(0xFF3A3A3C);
        LinearLayout.LayoutParams prevParams = new LinearLayout.LayoutParams(dp(50), dp(50));
        prevBtn.setLayoutParams(prevParams);
        prevBtn.setOnClickListener(v -> {
            try {
                currentCalendar.add(Calendar.MONTH, -1);
                updateCalendar();
            } catch (Exception ignored) {}
        });
        navRow.addView(prevBtn);

        monthYearText = new TextView(context);
        monthYearText.setTextColor(0xFFFFFFFF);
        monthYearText.setTextSize(18);
        monthYearText.setTypeface(null, Typeface.BOLD);
        monthYearText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        navRow.addView(monthYearText, monthParams);

        Button nextBtn = new Button(context);
        nextBtn.setText("▶");
        nextBtn.setTextSize(18);
        nextBtn.setTypeface(null, Typeface.BOLD);
        nextBtn.setTextColor(0xFFFFFFFF);
        nextBtn.setBackgroundColor(0xFF3A3A3C);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(dp(50), dp(50));
        nextBtn.setLayoutParams(nextParams);
        nextBtn.setOnClickListener(v -> {
            try {
                currentCalendar.add(Calendar.MONTH, 1);
                updateCalendar();
            } catch (Exception ignored) {}
        });
        navRow.addView(nextBtn);

        addView(navRow);

        // Day headers
        GridLayout dayHeaders = new GridLayout(context);
        dayHeaders.setColumnCount(7);
        String[] dayNames = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        for (String day : dayNames) {
            TextView dayText = new TextView(context);
            dayText.setText(day);
            dayText.setTextColor(0xFF8E8E93);
            dayText.setTextSize(12);
            dayText.setGravity(Gravity.CENTER);
            dayText.setTypeface(null, Typeface.BOLD);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(40);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dayText.setLayoutParams(params);
            dayHeaders.addView(dayText);
        }
        addView(dayHeaders);

        // Calendar grid
        calendarGrid = new GridLayout(context);
        calendarGrid.setColumnCount(7);
        LayoutParams gridParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        gridParams.bottomMargin = dp(12);
        addView(calendarGrid, gridParams);

        // Selected date display
        selectedDateText = new TextView(context);
        selectedDateText.setTextColor(0xFFFFFFFF);
        selectedDateText.setTextSize(16);
        selectedDateText.setGravity(Gravity.CENTER);
        selectedDateText.setBackgroundColor(0xFF2C2C2E);
        selectedDateText.setPadding(dp(12), dp(12), dp(12), dp(12));
        LayoutParams dateParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dateParams.bottomMargin = dp(12);
        android.graphics.drawable.GradientDrawable dateBg = new android.graphics.drawable.GradientDrawable();
        dateBg.setCornerRadius(dp(8));
        dateBg.setColor(0xFF2C2C2E);
        selectedDateText.setBackground(dateBg);
        addView(selectedDateText, dateParams);

        // Insert button
        Button insertBtn = new Button(context);
        insertBtn.setText("✓ Yapıştır");
        insertBtn.setTextColor(0xFFFFFFFF);
        insertBtn.setTextSize(16);
        insertBtn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable insertBg = new android.graphics.drawable.GradientDrawable();
        insertBg.setCornerRadius(dp(12));
        insertBg.setColor(0xFF34C759);
        insertBtn.setBackground(insertBg);
        LayoutParams insertParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(50));
        insertBtn.setLayoutParams(insertParams);
        insertBtn.setOnClickListener(v -> {
            try {
                if (callback != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", new Locale("tr"));
                    String date = sdf.format(selectedCalendar.getTime());
                    callback.onDateSelected(date);
                    callback.onClose();
                }
            } catch (Exception ignored) {}
        });
        addView(insertBtn);

        updateCalendar();
    }

    private void updateCalendar() {
        try {
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("tr"));
            monthYearText.setText(monthFormat.format(currentCalendar.getTime()));

            calendarGrid.removeAllViews();

            Calendar cal = (Calendar) currentCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int offset = (firstDayOfWeek + 5) % 7; // Adjust for Monday start

            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // Empty cells before month start
            for (int i = 0; i < offset; i++) {
                calendarGrid.addView(createDayCell("", false, false));
            }

            // Days of month
            for (int day = 1; day <= daysInMonth; day++) {
                boolean isToday = (day == today && 
                                 cal.get(Calendar.MONTH) == currentMonth && 
                                 cal.get(Calendar.YEAR) == currentYear);
                calendarGrid.addView(createDayCell(String.valueOf(day), true, isToday));
            }

            updateSelectedDate();
        } catch (Exception ignored) {}
    }

    private TextView createDayCell(String text, boolean enabled, boolean isToday) {
        TextView cell = new TextView(getContext());
        cell.setText(text);
        cell.setTextColor(enabled ? 0xFFFFFFFF : 0xFF3A3A3C);
        cell.setTextSize(14);
        cell.setGravity(Gravity.CENTER);
        cell.setTypeface(null, isToday ? Typeface.BOLD : Typeface.NORMAL);
        
        try {
            if (isToday) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                bg.setColor(0xFF007AFF);
                cell.setBackground(bg);
            } else if (enabled) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setCornerRadius(dp(8));
                bg.setColor(0xFF2C2C2E);
                cell.setBackground(bg);
            }
        } catch (Exception ignored) {}

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(50);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(params);

        if (enabled) {
            try {
                int day = Integer.parseInt(text);
                cell.setOnClickListener(v -> {
                    try {
                        selectedCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
                        selectedCalendar.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                        updateSelectedDate();
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }

        return cell;
    }

    private void updateSelectedDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, EEEE", new Locale("tr"));
            selectedDateText.setText("📅 " + sdf.format(selectedCalendar.getTime()));
        } catch (Exception ignored) {}
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

