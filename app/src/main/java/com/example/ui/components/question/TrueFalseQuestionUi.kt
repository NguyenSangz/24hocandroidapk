package com.example.ui.components.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

/**
 * Giao diện câu hỏi Đúng / Sai: hai nút lớn thân thiện cho học sinh THCS.
 */
@Composable
fun TrueFalseQuestionUi(
    question: Question.TrueFalse,
    userAnswer: UserAnswer.TrueFalse?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onAnswerSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedValue = userAnswer?.selectedValue
    val isGraded = gradeResult != null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nút Đúng
        TrueFalseButton(
            label = "Đúng",
            isTrue = true,
            isSelected = selectedValue == true,
            isCorrectAnswer = question.correctAnswer,
            isGraded = isGraded,
            enabled = !isReadOnly,
            onClick = { onAnswerSelected(true) },
            modifier = Modifier
                .weight(1f)
                .testTag("tf_true_button")
        )

        // Nút Sai
        TrueFalseButton(
            label = "Sai",
            isTrue = false,
            isSelected = selectedValue == false,
            isCorrectAnswer = !question.correctAnswer,
            isGraded = isGraded,
            enabled = !isReadOnly,
            onClick = { onAnswerSelected(false) },
            modifier = Modifier
                .weight(1f)
                .testTag("tf_false_button")
        )
    }
}

@Composable
private fun TrueFalseButton(
    label: String,
    isTrue: Boolean,
    isSelected: Boolean,
    isCorrectAnswer: Boolean,
    isGraded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isTrue) CorrectGreen else IncorrectRed
    val isSelectedWrong = isGraded && isSelected && !isCorrectAnswer

    val containerColor = when {
        isGraded && isCorrectAnswer -> Color(0xFFDCFCE7)
        isGraded && isSelectedWrong -> Color(0xFFFFE4E6)
        isSelected -> baseColor
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isGraded && isCorrectAnswer -> CorrectGreen
        isGraded && isSelectedWrong -> IncorrectRed
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isGraded && isCorrectAnswer -> CorrectGreen
        isGraded && isSelectedWrong -> IncorrectRed
        isSelected -> baseColor
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected || (isGraded && isCorrectAnswer)) 2.5.dp else 1.5.dp, borderColor),
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isTrue) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrueFalseQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.TrueFalse(
            id = "tf1",
            lessonId = "l1",
            content = "Hình thoi có hai đường chéo vuông góc với nhau.",
            correctAnswer = true
        )
        TrueFalseQuestionUi(
            question = sample,
            userAnswer = UserAnswer.TrueFalse(true),
            gradeResult = null,
            isReadOnly = false,
            onAnswerSelected = {}
        )
    }
}
