---
description: Update agent documentation based on codebase changes
---

# Documentation Update Workflow

This workflow guides updating the `.agent/` documentation to reflect codebase changes.

## When to Update

Run this workflow when:

- ✅ Adding new modules or significant features
- ✅ Changing API endpoints
- ✅ Modifying database schema
- ✅ Adding new workflows or processes
- ✅ Resolving issues that should become SOPs
- ✅ Completing a feature that should be documented in `/task`

---

## Quick Update Procedure

### Step 1: Identify What Changed

Review recent changes:

```bash
# Recent commits
git log --oneline -20

# Changed files
git diff --name-only HEAD~10

# Files changed in specific area
git diff --name-only HEAD~10 -- newm-server/
```

### Step 2: Update Relevant Documentation

Based on changes, update the appropriate files:

| Change Type | Files to Update |
|-------------|-----------------|
| Architecture changes | `.agent/system/architecture.md` |
| Database changes | `.agent/system/database-schema.md` |
| API changes | `.agent/system/api-endpoints.md` |
| New workflow | `.agent/workflows/{name}.md` |
| Resolved issue | `.agent/SOPs/{category}/{name}.md` |
| Completed feature | `.agent/task/{domain}/{feature}.md` |

### Step 3: Update Index

If adding new files, update `.agent/readme.md` to include them.

### Step 4: Commit Documentation

```bash
git add .agent/
git commit -m "docs: Update agent documentation"
```

---

## Documentation Templates

### New Workflow

Create `.agent/workflows/{name}.md`:

```markdown
---
description: Brief description for index
---

# {Workflow Name} Workflow

Description of the workflow.

## Quick Commands

\`\`\`bash
# Primary command
command here
\`\`\`

## Detailed Steps

### Step 1: {Title}
// turbo (if safe to auto-run)
\`\`\`bash
command
\`\`\`

...
```

### New SOP

Create `.agent/SOPs/{category}/{name}.md`:

```markdown
# SOP: {Name}

## Overview
Brief description.

## Prerequisites
- Requirement 1
- Requirement 2

## Procedure

### Step 1: {Title}
Description and commands.

## Common Pitfalls

### ⚠️ {Issue}
**Problem:** Description  
**Solution:** How to fix

## Verification
How to confirm success.

---
**Created:** {Date}
```

### New Task Plan

Create `.agent/task/{domain}/{feature}.md`:

```markdown
# {Feature Name}

## Overview
Brief description.

## Requirements
- [ ] Requirement 1
- [ ] Requirement 2

## Technical Design
...

## Implementation Steps
1. Step 1
2. Step 2

---
**Status:** ✅ Completed  
**Date:** {Date}
```

---

## Bulk Updates

### After Major Refactoring

1. Review all files in `.agent/system/`
2. Update architecture diagrams
3. Verify API documentation accuracy
4. Check database schema documentation

### After API Changes

```bash
# Find all controller changes
git diff --name-only HEAD~20 | grep -E "Routes"
```

Update `.agent/system/api-endpoints.md` with new/changed endpoints.

### After Database Migration

Update `.agent/system/database-schema.md`:
- Add new entities
- Update relationships
- Document migration notes

---

## Validation

After updating, verify documentation:

1. **Links work** — Check all internal links
2. **Commands work** — Test command examples
3. **Accuracy** — Verify information matches code
4. **Completeness** — Ensure all new features documented

---

## Automation Ideas

Consider automating:

- API documentation generation from OpenAPI/Swagger
- Database schema documentation from entity classes
- Module dependency graphs from Gradle

---

## Commit Message Format

```
docs: Update {area} documentation

- Added {new thing}
- Updated {changed thing}
- Fixed {corrected thing}
```
