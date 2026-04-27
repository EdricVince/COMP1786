using Newtonsoft.Json.Linq;
using ProjectExpenseTracker.Models;
using System.Text;

namespace ProjectExpenseTracker.Services;

public class FirebaseService
{
    private const string PROJECT_ID = "comp1786-project-87a99";
    private const string BASE_URL   =
        $"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents";

    private readonly HttpClient _http;

    public FirebaseService()
    {
        _http = new HttpClient { Timeout = TimeSpan.FromSeconds(20) };
    }

    // ── Fetch projects WITH expenses so TotalSpent is accurate ───────────────
    public async Task<List<Project>> GetProjectsAsync()
    {
        var result = new List<Project>();
        try
        {
            var json = await _http.GetStringAsync($"{BASE_URL}/projects");
            var docs = JObject.Parse(json)["documents"] as JArray;
            if (docs == null) return result;

            // Load each project + its expenses in parallel
            var tasks = docs.Select(async doc =>
            {
                var p = ParseProject(doc);
                if (p == null) return null;
                p.Expenses = await GetExpensesAsync(p.FirestoreId);
                return p;
            });

            var projects = await Task.WhenAll(tasks);
            result.AddRange(projects.Where(p => p != null)!);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[FirebaseService] GetProjects: {ex.Message}");
        }
        return result;
    }

    // ── Fetch expenses for one project ────────────────────────────────────────
    public async Task<List<Expense>> GetExpensesAsync(string projectDocId)
    {
        var result = new List<Expense>();
        try
        {
            var json = await _http.GetStringAsync(
                $"{BASE_URL}/projects/{projectDocId}/expenses");
            var docs = JObject.Parse(json)["documents"] as JArray;
            if (docs == null) return result;
            foreach (var doc in docs)
            {
                var e = ParseExpense(doc);
                if (e != null) result.Add(e);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[FirebaseService] GetExpenses({projectDocId}): {ex.Message}");
        }
        return result;
    }

    // ── Delete ALL projects (and their expenses sub-collections) ─────────────
    public async Task<bool> DeleteAllProjectsAsync()
    {
        try
        {
            var json = await _http.GetStringAsync($"{BASE_URL}/projects");
            var docs = JObject.Parse(json)["documents"] as JArray;
            if (docs == null) return true;

            foreach (var doc in docs)
            {
                var docName = doc["name"]?.ToString() ?? "";
                var docId   = docName.Split('/').Last();

                // Delete expenses first
                try
                {
                    var expJson = await _http.GetStringAsync(
                        $"{BASE_URL}/projects/{docId}/expenses");
                    var expDocs = JObject.Parse(expJson)["documents"] as JArray;
                    if (expDocs != null)
                    {
                        foreach (var exp in expDocs)
                        {
                            var expName = exp["name"]?.ToString() ?? "";
                            var url = $"https://firestore.googleapis.com/v1/{expName}";
                            await _http.DeleteAsync(url);
                        }
                    }
                }
                catch { }

                // Delete project
                var projectUrl = $"https://firestore.googleapis.com/v1/{docName}";
                await _http.DeleteAsync(projectUrl);
            }
            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[FirebaseService] DeleteAll: {ex.Message}");
            return false;
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────────
    private static Project? ParseProject(JToken doc)
    {
        try
        {
            var fields  = doc["fields"] as JObject;
            if (fields == null) return null;
            var docPath = doc["name"]?.ToString() ?? "";
            return new Project
            {
                FirestoreId         = docPath.Split('/').Last(),
                Code                = Str(fields, "code"),
                Name                = Str(fields, "name"),
                Description         = Str(fields, "description"),
                StartDate           = Str(fields, "startDate"),
                EndDate             = Str(fields, "endDate"),
                Manager             = Str(fields, "manager"),
                Status              = Str(fields, "status"),
                Budget              = Dbl(fields, "budget"),
                SpecialRequirements = Str(fields, "specialRequirements"),
                ClientInfo          = Str(fields, "clientInfo"),
                CreatedAt           = Str(fields, "createdAt"),
            };
        }
        catch { return null; }
    }

    private static Expense? ParseExpense(JToken doc)
    {
        try
        {
            var fields  = doc["fields"] as JObject;
            if (fields == null) return null;
            var docPath = doc["name"]?.ToString() ?? "";
            return new Expense
            {
                FirestoreId   = docPath.Split('/').Last(),
                Code          = Str(fields, "code"),
                Date          = Str(fields, "date"),
                Amount        = Dbl(fields, "amount"),
                Currency      = Str(fields, "currency"),
                Type          = Str(fields, "type"),
                PaymentMethod = Str(fields, "paymentMethod"),
                Claimant      = Str(fields, "claimant"),
                Status        = Str(fields, "status"),
                Description   = Str(fields, "description"),
                Location      = Str(fields, "location"),
                CreatedAt     = Str(fields, "createdAt"),
            };
        }
        catch { return null; }
    }

    private static string Str(JObject f, string key) =>
        f[key]?["stringValue"]?.ToString()
        ?? f[key]?["integerValue"]?.ToString()
        ?? string.Empty;

    private static double Dbl(JObject f, string key)
    {
        var raw = f[key]?["doubleValue"]?.ToString()
               ?? f[key]?["integerValue"]?.ToString()
               ?? "0";
        return double.TryParse(raw,
            System.Globalization.NumberStyles.Any,
            System.Globalization.CultureInfo.InvariantCulture,
            out var d) ? d : 0;
    }
}
