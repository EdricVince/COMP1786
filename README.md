# Project Expense Tracker — MAUI App
### COMP1786 | Feature G & H | University of Greenwich

## Folder Structure
```
ProjectExpenseTracker/
│
├── Models/                       ← Data models (mirroring Firestore documents)
│   ├── Project.cs
│   └── Expense.cs
│
├── Services/                     ← Business logic & data access
│   ├── FirebaseService.cs        ← Firestore REST API client
│   └── FavouritesService.cs      ← Local storage via MAUI Preferences
│
├── ViewModels/                   ← MVVM state & logic per screen
│   ├── Base/
│   │   └── BaseViewModel.cs      ← INotifyPropertyChanged + IsLoading/IsEmpty
│   ├── ProjectsViewModel.cs
│   ├── FavouritesViewModel.cs
│   └── ProjectDetailViewModel.cs
│
├── Views/                        ← XAML screens
│   ├── ProjectsView.xaml/.cs     ← Project list + search + filter
│   ├── FavouritesView.xaml/.cs   ← Starred projects
│   └── ProjectDetailView.xaml/.cs← Full detail + expenses
│
├── Resources/
│   └── Styles/
│       ├── Colors.xaml           ← All color tokens
│       └── Styles.xaml           ← Reusable control styles
│
├── App.xaml/.cs                  ← App entry, merges resource dictionaries
├── AppShell.xaml/.cs             ← Tab bar + route registration
└── MauiProgram.cs                ← DI container setup
```

## Firebase Config
File: `Services/FirebaseService.cs`
```csharp
private const string PROJECT_ID = "comp1786-project-87a99";
```

## How to Run
1. Open `ProjectExpenseTracker.csproj` in Visual Studio 2022
2. Wait for NuGet restore (Newtonsoft.Json)
3. Select Android Emulator or physical device
4. Press F5

## Features
| Feature | Description |
|---|---|
| G | Fetch projects from Firestore, search by name/date, filter by status |
| H | ♡ Favourite any project, stored locally, dedicated Favourites tab |
