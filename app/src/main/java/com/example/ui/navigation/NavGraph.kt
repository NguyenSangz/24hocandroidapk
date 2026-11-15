package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.exam.ExamTakingScreen
import com.example.ui.screens.flashcard.FlashcardStudyScreen
import com.example.ui.screens.grade.SelectGradeScreen
import com.example.ui.screens.grade.SelectGradeViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.practice.PracticeQuizScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.splash.SplashViewModel
import com.example.ui.screens.study.LessonDetailScreen
import com.example.ui.viewmodel.AppViewModelProvider
import com.example.ui.viewmodel.ExamViewModel
import com.example.ui.viewmodel.FlashcardViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PracticeViewModel
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val selectedGrade by mainViewModel.selectedGrade.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = viewModel(factory = AppViewModelProvider.Factory)
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToNext = { isGradeSelected ->
                    if (!isGradeSelected) {
                        navController.navigate(Screen.WelcomeAuth.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.WelcomeAuth.route) {
            val accountViewModel: com.example.ui.screens.account.AccountViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.auth.WelcomeAuthScreen(
                viewModel = accountViewModel,
                onAuthSuccess = { needsParentalConsent ->
                    if (needsParentalConsent) {
                        navController.navigate(Screen.ParentalConsent.route) {
                            popUpTo(Screen.WelcomeAuth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.SelectGrade.route) {
                            popUpTo(Screen.WelcomeAuth.route) { inclusive = true }
                        }
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(Screen.SelectGrade.route) {
                        popUpTo(Screen.WelcomeAuth.route) { inclusive = true }
                    }
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        composable(Screen.ParentalConsent.route) {
            val accountViewModel: com.example.ui.screens.account.AccountViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.auth.ParentalConsentScreen(
                viewModel = accountViewModel,
                onConsentSuccess = {
                    navController.navigate(Screen.SelectGrade.route) {
                        popUpTo(Screen.ParentalConsent.route) { inclusive = true }
                    }
                },
                onSkipOrLater = {
                    navController.navigate(Screen.SelectGrade.route) {
                        popUpTo(Screen.ParentalConsent.route) { inclusive = true }
                    }
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        composable(Screen.SelectGrade.route) {
            val selectGradeViewModel: SelectGradeViewModel = viewModel(factory = AppViewModelProvider.Factory)
            SelectGradeScreen(
                viewModel = selectGradeViewModel,
                onGradeConfirmed = { grade ->
                    mainViewModel.setGrade(grade)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SelectGrade.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                mainViewModel = mainViewModel,
                onNavigateToLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                },
                onNavigateToSubjectChapterList = { subjectId ->
                    navController.navigate(Screen.ChapterList.createRoute(subjectId))
                },
                onStartPracticeQuiz = { lessonId, subjectId, chapterId, count, difficulty ->
                    navController.navigate(Screen.PracticeQuiz.createRoute(lessonId, subjectId, count, difficulty, chapterId))
                },
                onStartExam = { subjectId, title ->
                    navController.navigate(Screen.ExamTaking.createRoute(subjectId, title))
                },
                onOpenFlashcards = {
                    navController.navigate(Screen.FlashcardStudy.route)
                },
                onOpenWrongBank = {
                    navController.navigate(Screen.WrongBank.route)
                },
                onNavigateToSelectGrade = {
                    navController.navigate(Screen.SelectGrade.route)
                },
                onNavigateToSearch = { initialQuery ->
                    navController.navigate(Screen.Search.createRoute(initialQuery))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAccount = {
                    navController.navigate(Screen.Account.route)
                },
                onNavigateToAiTutor = {
                    navController.navigate(Screen.AiTutor.route)
                }
            )
        }

        composable(
            route = Screen.ChapterList.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "math"
            val chapterViewModel: com.example.ui.screens.study.ChapterListViewModel =
                viewModel(factory = AppViewModelProvider.provideChapterListViewModel(subjectId))
            com.example.ui.screens.study.ChapterListScreen(
                viewModel = chapterViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                }
            )
        }

        composable(
            route = Screen.LessonDetail.route,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonViewModel: com.example.ui.screens.study.LessonViewModel =
                viewModel(factory = AppViewModelProvider.provideLessonViewModel(lessonId))
            LessonDetailScreen(
                lessonId = lessonId,
                viewModel = lessonViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnotherLesson = { nextId ->
                    navController.navigate(Screen.LessonDetail.createRoute(nextId)) {
                        popUpTo(Screen.LessonDetail.route) { inclusive = true }
                    }
                },
                onStartPractice = { lId ->
                    navController.navigate(Screen.PracticeQuiz.createRoute(lId, "none"))
                }
            )
        }

        composable(
            route = Screen.PracticeQuiz.route,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType; defaultValue = "none" },
                navArgument("subjectId") { type = NavType.StringType; defaultValue = "none" },
                navArgument("count") { type = NavType.IntType; defaultValue = 10 },
                navArgument("difficulty") { type = NavType.StringType; defaultValue = "MIXED" },
                navArgument("chapterId") { type = NavType.StringType; defaultValue = "all" }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: "none"
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "none"
            val count = backStackEntry.arguments?.getInt("count") ?: 10
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "MIXED"
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: "all"
            val practiceViewModel: PracticeViewModel = viewModel(factory = AppViewModelProvider.Factory)
            PracticeQuizScreen(
                lessonId = lessonId,
                subjectId = subjectId,
                chapterId = chapterId,
                count = count,
                difficulty = difficulty,
                viewModel = practiceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReviewSession = { wrongIds ->
                    val qIdsParam = wrongIds.joinToString(",")
                    navController.navigate(
                        Screen.ReviewSession.createRoute(
                            subjectId = if (subjectId != "none") subjectId else "none",
                            count = wrongIds.size,
                            questionIds = qIdsParam,
                            title = "Làm lại ${wrongIds.size} câu sai vừa làm"
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.ExamTaking.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "all"
            val title = backStackEntry.arguments?.getString("title") ?: "Đề kiểm tra"
            val examViewModel: ExamViewModel = viewModel(factory = AppViewModelProvider.Factory)
            ExamTakingScreen(
                subjectId = subjectId,
                title = title,
                viewModel = examViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRetryWrongQuestions = { wrongIds ->
                    val qIdsParam = wrongIds.joinToString(",")
                    navController.navigate(
                        Screen.ReviewSession.createRoute(
                            subjectId = subjectId,
                            count = wrongIds.size,
                            questionIds = qIdsParam,
                            title = "Ôn lại ${wrongIds.size} câu sai của đề thi"
                        )
                    )
                }
            )
        }

        composable(Screen.FlashcardStudy.route) {
            val flashcardViewModel: FlashcardViewModel = viewModel(factory = AppViewModelProvider.Factory)
            FlashcardStudyScreen(
                viewModel = flashcardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WrongBank.route) {
            val wrongBankViewModel: com.example.ui.viewmodel.WrongBankViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.wrongbank.WrongBankScreen(
                viewModel = wrongBankViewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartReviewSession = { sId, cId, count, qIds, title ->
                    val qIdsParam = qIds?.joinToString(",") ?: "none"
                    navController.navigate(
                        Screen.ReviewSession.createRoute(
                            subjectId = sId ?: "none",
                            chapterId = cId ?: "all",
                            count = count,
                            questionIds = qIdsParam,
                            title = title
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.ReviewSession.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType; defaultValue = "none" },
                navArgument("chapterId") { type = NavType.StringType; defaultValue = "all" },
                navArgument("count") { type = NavType.IntType; defaultValue = 10 },
                navArgument("questionIds") { type = NavType.StringType; defaultValue = "none" },
                navArgument("title") { type = NavType.StringType; defaultValue = "Ôn tập câu sai" }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "none"
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: "all"
            val count = backStackEntry.arguments?.getInt("count") ?: 10
            val questionIdsParam = backStackEntry.arguments?.getString("questionIds") ?: "none"
            val title = backStackEntry.arguments?.getString("title") ?: "Ôn tập câu sai"
            val questionIdsList = if (questionIdsParam != "none" && questionIdsParam.isNotBlank()) {
                questionIdsParam.split(",").filter { it.isNotBlank() }
            } else null

            val reviewViewModel: com.example.ui.viewmodel.ReviewSessionViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.wrongbank.ReviewSessionScreen(
                grade = selectedGrade,
                initialSubjectId = subjectId,
                initialChapterId = chapterId,
                initialCount = count,
                initialQuestionIds = questionIdsList,
                initialTitle = title,
                viewModel = reviewViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
        ) {
            val searchViewModel: com.example.ui.screens.search.SearchViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.search.SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { lessonId ->
                    navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                },
                onNavigateToChapter = { subjectId ->
                    navController.navigate(Screen.ChapterList.createRoute(subjectId))
                },
                onStartPracticeQuiz = { lessonId, subjectId ->
                    navController.navigate(Screen.PracticeQuiz.createRoute(lessonId, subjectId))
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: com.example.ui.screens.settings.SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.settings.SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToRestore = { navController.navigate(Screen.Restore.route) },
                onNavigateToPdfReport = { navController.navigate(Screen.PdfReport.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToDemoLogin = { navController.navigate(Screen.DemoLogin.route) }
            )
        }

        composable(Screen.Account.route) {
            val accountViewModel: com.example.ui.screens.account.AccountViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.account.AccountScreen(
                viewModel = accountViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }

        composable(Screen.DemoLogin.route) {
            val accountViewModel: com.example.ui.screens.account.AccountViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.auth.DemoLoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.DemoLogin.route) { inclusive = true }
                    }
                },
                viewModel = accountViewModel
            )
        }

        composable(Screen.Backup.route) {
            com.example.ui.screens.settings.BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Restore.route) {
            com.example.ui.screens.settings.RestoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfReport.route) {
            com.example.ui.screens.settings.PdfReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            com.example.ui.screens.settings.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            com.example.ui.screens.legal.PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiTutor.route) {
            val aiTutorViewModel: com.example.ui.screens.ai.AiTutorViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            com.example.ui.screens.ai.AiTutorScreen(
                viewModel = aiTutorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTool = { route -> navController.navigate(route) },
                onNavigateToPractice = {
                    navController.navigate(Screen.PracticeQuiz.createRoute())
                },
                onNavigateToFlashcards = {
                    navController.navigate(Screen.FlashcardStudy.route)
                }
            )
        }
    }
}
