import subprocess

def git_push_main(commit_message: str):
    """
    현재 Git 레포지토리에서 변경사항을 add, commit, push origin main까지 수행.
    SSH 인증이 되어 있어야 함.
    """
    try:
        # 모든 변경사항 add
        subprocess.run(["git", "add", "."], check=True)
        
        # commit
        subprocess.run(["git", "commit", "-m", commit_message], check=True)
        
        # push to origin main
        subprocess.run(["git", "push", "origin", "main"], check=True)
        
        print("✅ Git push to origin/main completed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"❌ Git command failed: {e}")