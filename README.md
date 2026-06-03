# WC26 result data server

This is the Vercel root directory.

Keep this structure exactly:

```text
Server/
  vercel.json
  public/
    data/
      results.csv
```

The only file you maintain after deployment is:

```text
Server/public/data/results.csv
```

Required CSV header:

```text
match_id,team1,team2,score1,score2,penalty1,penalty2
```

Rules:

- Keep all team names in ASCII English letters.
- For group-stage matches, team1 and team2 are already filled.
- For result updates, fill score1 and score2.
- For penalty shootouts, fill penalty1 and penalty2.
- Leave scores blank for unplayed matches.

After Vercel deployment, the app should use:

```text
https://YOUR_VERCEL_PROJECT.vercel.app/data/results.csv
```

You may upload the whole WC26 project to GitHub. In Vercel, set Root Directory to:

```text
Server
```
