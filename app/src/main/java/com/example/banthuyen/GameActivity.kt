package com.example.banthuyen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tạo model
        val model = GameModel(this)

        // Tạo view trước (chưa gắn controller)
        val view = GameView(this, model)

        // Tạo controller và gắn vào view
        val controller = GameController(model, view, this)
        view.setController(controller)

        // Hiển thị view lên màn hình
        setContentView(view)
    }
}
