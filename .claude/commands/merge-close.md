Merge and clean up the branch for issue $ARGUMENTS:

1. Find the branch name: `git branch | grep $ARGUMENTS`
2. From the main worktree, merge the branch: `git merge <branch-name>`
3. Remove the worktree: `git worktree remove <worktree-path>`
4. Close the issue: `gh issue close $ARGUMENTS`
5. Delete the local branch: `git branch -d <branch-name>`
