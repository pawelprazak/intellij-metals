package intellij.metals.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, DefaultActionGroup, Separator}
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerListener}
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.{StatusBarWidget, StatusBarWidgetFactory, WindowManager}
import com.intellij.platform.lsp.api.{LspServerListener, LspServerManager, LspServerManagerListener}
import com.intellij.platform.lsp.impl.LspServerManagerImpl
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.messages.MessageBusConnection
import intellij.metals.MetalsLspServerProvider
import intellij.metals.server.LspServerUtils
import intellij.metals.settings.{MetalsConfigurable, MetalsProjectSettings, MetalsSettings}
import org.jetbrains.bsp.BspBundle
import org.jetbrains.bsp.protocol.BspCommunicationService

import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

final class MetalsStatusWidgetFactory extends StatusBarWidgetFactory {

  override def getId: String = "Metals LSP"

  override def getDisplayName: String = "Metals (Scala LSP)"

  override def createWidget(project: Project): StatusBarWidget =
    new MetalsStatusWidget(project)

  private class MetalsStatusWidget(project: Project)
      extends EditorBasedWidget(project)
      with FileEditorManagerListener
      with Consumer[MouseEvent] { self =>

    override def ID(): String = s"Metals LSP: ${project.getName}"

    override def getPresentation: StatusBarWidget.WidgetPresentation =
      new StatusBarWidget.IconPresentation {
        private def isMetalsEnabled = {
            val settings = MetalsProjectSettings.getInstance(project)
            settings.metalsEnabled
        }

        override def getIcon: Icon = {
          isMetalsEnabled match {
            case true  => Icons.Metals
            case false => Icons.MetalsDisabled
          }
        }

        override def getTooltipText: String =
          LspServerUtils.forProject(project) match {
            case Some(_) => "Metals is running"
            case None => "Metals is disconnected"
          }



        override def getClickConsumer: Consumer[MouseEvent] = self
      }

    private val configureMetalsAction = new AnAction("Configure Metals executable") {
      getTemplatePresentation.setIcon(Icons.Metals)

      override def actionPerformed(e: AnActionEvent): Unit = {
        val showSettingsUtil   = ShowSettingsUtil.getInstance()
        val metalsConfigurable = new MetalsConfigurable(project)
        showSettingsUtil.editConfigurable(project, metalsConfigurable)
      }
    }

    private val connectAction = new AnAction() {
      override def update(e: AnActionEvent): Unit = {
        val metalsPath = MetalsSettings.getInstance.metalsPath
        if (metalsPath.isEmpty) {
          configureMetalsAction.actionPerformed(e)
        } else {
          val isConnected = LspServerUtils.forProject(project).isDefined
          if (isConnected) {
            e.getPresentation.setText("Disconnect")
          } else {
            e.getPresentation.setText("Connect to Metals")
          }
        }
      }

      override def actionPerformed(e: AnActionEvent): Unit = {
        e.getPresentation.getText match {
          case "Connect to Metals" =>
            LspServerManagerImpl
              .getInstanceImpl(project)
              .startServersIfNeeded(classOf[MetalsLspServerProvider])
            MetalsProjectSettings.getInstance(project).metalsEnabled = true
          case "Disconnect" =>
            LspServerManagerImpl
              .getInstanceImpl(project)
              .stopServers(classOf[MetalsLspServerProvider])
            MetalsProjectSettings.getInstance(project).metalsEnabled = false
        }
        WindowManager.getInstance.getStatusBar(project).updateWidget(ID())
      }
    }

    override def consume(e: MouseEvent): Unit = {
      val group = new DefaultActionGroup()
      group.add(connectAction)
      group.add(new Separator())
      group.add(configureMetalsAction)

      val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
      val context   = DataManager.getInstance.getDataContext(e.getComponent)
      val popup     = JBPopupFactory.getInstance.createActionGroupPopup("Metals", group, context, mnemonics, true)
      val dimension = popup.getContent.getPreferredSize
      val at        = new Point(0, -dimension.height)
      popup.show(new RelativePoint(e.getComponent, at))
    }
  }
}
