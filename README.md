[![PRs and merges to master](https://github.com/ingzkp/zk-notary/actions/workflows/on-push.yml/badge.svg)](https://github.com/ingzkp/zk-notary/actions/workflows/on-push.yml) [![Nightly build](https://github.com/ingzkp/zk-notary/actions/workflows/scheduled-all.yml/badge.svg)](https://github.com/ingzkp/zk-notary/actions/workflows/scheduled-all.yml)

## Testing best practices

We follow most of the best practices defined here: https://phauer.com/2018/best-practices-unit-testing-kotlin/.
We use some different libs for assertions and mocks, but the principles remain the same.

### Test Class lifecycle

We have configured JUnit5 so that test classes are instantiated only once. This means 'static' setup can just go in the init block or by declaring vals. If you want to isolate individual tests more, you can still have a setup method that runs before or after each test. Just annotate it with `@BeforeEach` or `@AfterEach` (JUnit4's `@Before` will no longer work).

Unfortunately this does not apply cleanup that needs to happen after all tests in a class have completed: for that you will still need a function annotated with `@AfterAll`, since there is no destroy on Kotlin classes where this could be placed.

## A note on commit messages

For consistency, it is important that we use one standard for our commit messages.

For wording, we use the same standard as git itself. This is taken from: https://github.com/git/git/blob/master/Documentation/SubmittingPatches

> Describe your changes in imperative mood, e.g. "make xyzzy do frotz"
> instead of "[This patch] makes xyzzy do frotz" or "[I] changed xyzzy
> to do frotz", as if you are giving orders to the codebase to change
> its behavior.

Think of it this way: the commit message describes what will happen when a commit is applied.

For format we follow the advice from the `git commit` [manpage](https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-commit.html#_discussion):

> Though not required, it’s a good idea to begin the commit message with a single short (less than 50 character) line summarizing the change, followed by a blank line and then a more thorough description. The text up to the first blank line in a commit message is treated as the commit title, and that title is used throughout Git.

Like other holy wars, such as tabs versus spaces, VIM vs Emacs, etc., this can be argued about. Let's not.

## Branching strategy

In this repo, we will use Github Flow: https://guides.github.com/introduction/flow/

The aim is to have a commit history that looks like this, where feature branches always have linear history and no merge commits inside themselves.
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 89e8283 - Split DigestService for leaves and nodes 
| * 2f5b0bc - Add limitations doc
|/
*   dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
|\
| * 4ff33b7 - First version of e2e notarisation protocol
|/
*   ff0db31 - Merge pull request #13 from ingzkp/feature/zkwtx 
|\
| * db8d499 - Add ZKProverTx, ZKVerifierTx and JSON serialization 
|/
* 75b47d0 - Change Github Actions cache key 
```
One of the challenges in creating a meaningful history like this, is in keeping feature branches up to date with master without polluting their history with merge commits, and therefore polluting master when they are merged to master. Keeping a feature branch up to date by merging master into it regularly causes lots of merge commits from master like this This is not what we want to see:
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 12ab43c - Merge branch 'master' of github.com
| |\
| | * 98gf55c - Foo bar Baz
| | |
| * | 2f5b0bc - Add limitations doc
|/ /
|/   
* dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
```
There are two ways to prevent this. The best option is changing your development workflow to use regular rebasing on master as explained here: https://www.atlassian.com/git/tutorials/merging-vs-rebasing. Alternatively, if you really hate rebasing and especially the merge conflicts it can cause, you can also clean up your feature branch by using only one final rebase when you are ready to merge your branch into master: `git rebase -i origin/master`. Rebasing in itself already makes your feature branch history linear, but when you do it interactively, you can then `fixup` commits that you may not want to end up visibly in master, or `reword` bad commit messages.