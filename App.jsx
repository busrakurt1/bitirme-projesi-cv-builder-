import { Routes, Route } from 'react-router-dom'
import './App.css'
import Home from './pages/Home'
import Dashboard from './pages/Dashboard'
import Profile from './pages/Profile'
import CVBuilder from './pages/CVBuilder'
import JobAnalysis from './pages/JobAnalysis'
import { ThemeProvider } from './contexts/ThemeContext'
import Chatbot from './components/chatbot/Chatbot'

function App() {
  return (
    <ThemeProvider>
      <div className="App">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/cv-builder" element={<CVBuilder />} />
          <Route path="/job-analysis" element={<JobAnalysis />} />  
        </Routes>
        <Chatbot />
      </div>
    </ThemeProvider>
  )
}

export default App