package com.freehands.assistant;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences preferences;
    private Spinner voiceTypeSpinner;
    private SeekBar voiceQualitySeekBar;
    private TextView qualityValueText;
    
    // Voice type options
    private String[] voiceTypes = {"Male", "Female", "Child"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        initializeViews();
        setupToolbar();
        loadCurrentSettings();
        setupListeners();
        setupTestVoiceButton();
    }
    
    private void initializeViews() {
        voiceTypeSpinner = findViewById(R.id.voice_type_spinner);
        voiceQualitySeekBar = findViewById(R.id.voice_quality_seekbar);
        qualityValueText = findViewById(R.id.quality_value_text);
        
        // Setup voice type spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            voiceTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceTypeSpinner.setAdapter(adapter);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Voice Settings");
        }
    }
    
    private void loadCurrentSettings() {
        // Load voice type setting
        String savedVoiceType = preferences.getString("voice_type", "Female");
        int voiceTypePosition = getVoiceTypePosition(savedVoiceType);
        voiceTypeSpinner.setSelection(voiceTypePosition);
        
        // Load voice quality setting (0-100)
        int voiceQuality = preferences.getInt("voice_quality", 75);
        voiceQualitySeekBar.setProgress(voiceQuality);
        updateQualityText(voiceQuality);
    }
    
    private int getVoiceTypePosition(String voiceType) {
        for (int i = 0; i < voiceTypes.length; i++) {
            if (voiceTypes[i].equals(voiceType)) {
                return i;
            }
        }
        return 1; // Default to Female
    }
    
    private void setupListeners() {
        // Voice type spinner listener
        voiceTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedVoiceType = voiceTypes[position];
                preferences.edit().putString("voice_type", selectedVoiceType).apply();
                
                // Update voice engine with new type
                VoiceEngine.getInstance().setVoiceType(selectedVoiceType);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Voice quality seekbar listener
        voiceQualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateQualityText(progress);
                    preferences.edit().putInt("voice_quality", progress).apply();
                    
                    // Update voice engine with new quality
                    VoiceEngine.getInstance().setVoiceQuality(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateQualityText(int quality) {
        String qualityLabel;
        if (quality < 25) {
            qualityLabel = "Low (" + quality + "%)";
        } else if (quality < 50) {
            qualityLabel = "Medium (" + quality + "%)";
        } else if (quality < 75) {
            qualityLabel = "High (" + quality + "%)";
        } else {
            qualityLabel = "Premium (" + quality + "%)";
        }
        qualityValueText.setText(qualityLabel);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupTestVoiceButton() {
        Button testVoiceButton = findViewById(R.id.test_voice_button);
        testVoiceButton.setOnClickListener(v -> testVoice());
    }
    
    private void testVoice() {
        VoiceEngine voiceEngine = VoiceEngine.getInstance();
        String testMessage = "Hello! This is how FreeHands sounds with your current voice settings.";
        voiceEngine.speak(testMessage);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Settings are automatically saved via listeners
    }
}