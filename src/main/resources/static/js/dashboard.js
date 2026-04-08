// TrimURL dashboard JS (legacy compat)
document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('analyticsModal')) {
    const modal = document.getElementById('analyticsModal');
    document.querySelector('.close-modal')?.addEventListener('click', () => modal.classList.add('hidden'));
    modal.addEventListener('click', e => { if (e.target === modal) modal.classList.add('hidden'); });
  }
});
