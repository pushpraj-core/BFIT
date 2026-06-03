import './style.css'

document.querySelector<HTMLDivElement>('#app')!.innerHTML = `
  <header>
    <div class="logo">
      BFIT <div class="logo-dot"></div>
    </div>
    <nav>
      <a href="#features">Features</a>
      <a href="#about">About</a>
      <a href="#download">Download</a>
    </nav>
    <button class="btn btn-secondary">Sign In</button>
  </header>

  <main>
    <div class="animate-slide-up delay-100">
      <h1>The Future of <br/><span class="text-gradient">Personal Fitness</span></h1>
    </div>
    
    <p class="subtitle animate-slide-up delay-200">
      Experience a handcrafted, intelligent fitness companion. Track meals with AI, log workouts seamlessly, and achieve your goals with a stunning, premium interface.
    </p>

    <div style="display: flex; gap: 1rem; margin-top: 1rem;" class="animate-slide-up delay-300">
      <button class="btn">Get Started</button>
      <button class="btn btn-secondary">Learn More</button>
    </div>

    <div class="features">
      <div class="glass-card animate-slide-up delay-400">
        <div class="feature-icon icon-purple">🤖</div>
        <h3>AI Meal Recognition</h3>
        <p style="color: var(--text-secondary); margin-top: 0.5rem; font-size: 0.95rem; line-height: 1.6;">
          Snap a photo of your food and let our advanced AI instantly identify calories and macros. Say goodbye to manual logging.
        </p>
      </div>

      <div class="glass-card animate-slide-up delay-500">
        <div class="feature-icon icon-cyan">📊</div>
        <h3>Smart Analytics</h3>
        <p style="color: var(--text-secondary); margin-top: 0.5rem; font-size: 0.95rem; line-height: 1.6;">
          Visualize your progress with beautiful, intuitive charts. Monitor weight trends, daily adherence, and macro breakdowns.
        </p>
      </div>

      <div class="glass-card animate-slide-up delay-600">
        <div class="feature-icon icon-pink">⚡</div>
        <h3>Personalized Plans</h3>
        <p style="color: var(--text-secondary); margin-top: 0.5rem; font-size: 0.95rem; line-height: 1.6;">
          Customized workout and diet plans tailored to your specific body type, goals, and dietary preferences.
        </p>
      </div>
    </div>
  </main>
`
