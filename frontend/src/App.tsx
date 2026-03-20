import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { LoginPage } from './auth/LoginPage';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppShell } from './layout/AppShell';
import { DashboardPage } from './reporting/DashboardPage';
import { ListView } from './records/ListView';
import { DetailView } from './records/DetailView';
import { RecordForm } from './records/RecordForm';
import { EntityBuilderPage } from './metadata/EntityBuilderPage';
import { PipelineBoard } from './sales/PipelineBoard';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="pipeline" element={<PipelineBoard />} />
          <Route path="o/:entityApiName" element={<ListView />} />
          <Route path="o/:entityApiName/new" element={<RecordForm />} />
          <Route path="o/:entityApiName/:id" element={<DetailView />} />
          <Route path="o/:entityApiName/:id/edit" element={<RecordForm />} />
          <Route path="setup/entities" element={<EntityBuilderPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
